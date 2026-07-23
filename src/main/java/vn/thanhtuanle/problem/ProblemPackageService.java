package vn.thanhtuanle.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.enums.ProblemStatus;
import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.common.util.FileUtil;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.problem.dto.CreateProblemDto;
import vn.thanhtuanle.problem.dto.ProblemPackageDto;
import vn.thanhtuanle.problem.dto.ProblemResponseDto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Parses and produces problem package zips (schemaVersion 1): problem.json at the root
 * (a single wrapping folder is tolerated) + testcases/N.in|N.out pairs. Creation is
 * delegated to {@link ProblemService#createProblem} so disk layout, info generation and
 * MinIO bundle publish stay on the single existing path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemPackageService {

    public static final int SCHEMA_VERSION = 1;
    private static final String PROBLEM_JSON = "problem.json";
    private static final Pattern TEST_CASE_ENTRY = Pattern.compile("(^|/)testcases/([^/]+\\.(in|out))$");

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ProblemService problemService;
    private final ProblemRepository problemRepository;

    record ParsedPackage(CreateProblemDto dto, Map<String, byte[]> testFiles) {
    }

    ParsedPackage parse(byte[] zipBytes) {
        Map<String, byte[]> entries = extract(zipBytes);

        byte[] problemJson = null;
        Map<String, byte[]> testFiles = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (name.contains("..")) {
                throw new IllegalArgumentException("Invalid package: unsafe entry name: " + name);
            }
            if (PROBLEM_JSON.equals(fileNameOf(name))) {
                problemJson = entry.getValue();
            } else {
                Matcher m = TEST_CASE_ENTRY.matcher(name);
                if (m.find()) {
                    testFiles.put(m.group(2), entry.getValue());
                }
            }
        }

        if (problemJson == null) {
            throw new IllegalArgumentException("Invalid package: problem.json not found");
        }

        CreateProblemDto dto = readProblemJson(problemJson);
        validate(dto);
        return new ParsedPackage(dto, keepCompletePairs(testFiles));
    }

    @Transactional
    public ProblemResponseDto importProblem(MultipartFile packageZip, String slugOverride) throws IOException {
        if (packageZip == null || packageZip.isEmpty()) {
            throw new IllegalArgumentException("Package file cannot be empty");
        }
        ParsedPackage parsed = parse(packageZip.getBytes());
        CreateProblemDto dto = parsed.dto();
        if (slugOverride != null && !slugOverride.isBlank()) {
            dto.setProblemSlug(slugOverride.trim());
        }
        log.info("Importing problem package as slug: {}", dto.getProblemSlug());
        return problemService.createProblem(dto, parsed.testFiles());
    }

    @Transactional(readOnly = true)
    public byte[] exportProblem(String slug) throws IOException {
        Problem problem = problemRepository.findByProblemSlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with slug: " + slug));

        CreateProblemDto dto = CreateProblemDto.builder()
                .title(problem.getTitle())
                .subject(problem.getSubject())
                .description(problem.getDescription())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit() != null ? problem.getMemoryLimit().intValue() : 0)
                .hardnessLevel(problem.getHardnessLevel())
                .problemSlug(problem.getProblemSlug())
                .inputDescription(problem.getInputDescription())
                .outputDescription(problem.getOutputDescription())
                .sampleInput(problem.getSampleInput())
                .sampleOutput(problem.getSampleOutput())
                .hint(problem.getHint())
                .status(ProblemStatus.fromValue(problem.getStatus()))
                .build();

        Map<String, byte[]> testFiles = new LinkedHashMap<>();
        for (TestCase tc : problem.getTestCases()) {
            testFiles.put(fileNameOf(tc.getInput()), readTestFile(tc.getInput()));
            testFiles.put(fileNameOf(tc.getOutput()), readTestFile(tc.getOutput()));
        }
        log.info("Exporting problem {} with {} test files", slug, testFiles.size());
        return buildPackageBytes(dto, testFiles);
    }

    byte[] buildPackageBytes(CreateProblemDto dto, Map<String, byte[]> testFiles) throws IOException {
        ProblemPackageDto pkg = ProblemPackageDto.builder()
                .schemaVersion(SCHEMA_VERSION)
                .problem(dto)
                .build();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry(PROBLEM_JSON));
            zos.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(pkg));
            zos.closeEntry();
            for (Map.Entry<String, byte[]> file : testFiles.entrySet()) {
                zos.putNextEntry(new ZipEntry("testcases/" + file.getKey()));
                zos.write(file.getValue());
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    /** Fail loudly: a backup with silently missing test data is worse than an error. */
    private static byte[] readTestFile(String relativePath) throws IOException {
        return Files.readAllBytes(Paths.get(AppProperties.TEST_CASE_DIR, relativePath));
    }

    private Map<String, byte[]> extract(byte[] zipBytes) {
        try {
            Map<String, byte[]> entries = FileUtil.extractZip(new ByteArrayInputStream(zipBytes));
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Invalid package: not a zip archive or empty");
            }
            return entries;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid package: not a zip archive", e);
        }
    }

    private CreateProblemDto readProblemJson(byte[] problemJson) {
        ProblemPackageDto pkg;
        try {
            pkg = objectMapper.readValue(problemJson, ProblemPackageDto.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid package: problem.json is not valid JSON: " + e.getMessage());
        }
        if (pkg.getSchemaVersion() != SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Invalid package: unsupported schemaVersion " + pkg.getSchemaVersion()
                            + " (expected " + SCHEMA_VERSION + ")");
        }
        if (pkg.getProblem() == null) {
            throw new IllegalArgumentException("Invalid package: problem.json has no \"problem\" object");
        }
        return pkg.getProblem();
    }

    private void validate(CreateProblemDto dto) {
        Set<ConstraintViolation<CreateProblemDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Invalid package: " + message);
        }
    }

    /** Keep only N.in files that have a matching N.out (and vice versa); require at least one pair. */
    private static Map<String, byte[]> keepCompletePairs(Map<String, byte[]> testFiles) {
        Map<String, byte[]> paired = new HashMap<>();
        testFiles.forEach((name, content) -> {
            if (name.endsWith(".in") && testFiles.containsKey(withExtension(name, ".out"))) {
                paired.put(name, content);
                paired.put(withExtension(name, ".out"), testFiles.get(withExtension(name, ".out")));
            }
        });
        if (paired.isEmpty()) {
            throw new IllegalArgumentException("Invalid package: no complete test-case pair (N.in + N.out) found");
        }
        return paired;
    }

    private static String withExtension(String inName, String extension) {
        return inName.substring(0, inName.lastIndexOf('.')) + extension;
    }

    private static String fileNameOf(String entryName) {
        int slash = entryName.lastIndexOf('/');
        return slash >= 0 ? entryName.substring(slash + 1) : entryName;
    }
}
