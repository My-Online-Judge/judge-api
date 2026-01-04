package vn.thanhtuanle.problem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.payload.PageResponse;
import vn.thanhtuanle.common.enums.ProblemStatus;
import vn.thanhtuanle.common.util.FileUtil;
import vn.thanhtuanle.common.util.GenerateTestCaseInfoUtil;
import vn.thanhtuanle.common.util.GenericSearchSpecificationUtil;
import vn.thanhtuanle.common.util.PropertyUtil;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.problem.dto.CreateProblemDto;
import vn.thanhtuanle.problem.dto.ProblemResponseDto;
import vn.thanhtuanle.problem.mapper.ProblemMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemMapper problemMapper;
    private final GenerateTestCaseInfoUtil infoGenerator;

    @Transactional
    public ProblemResponseDto createProblem(CreateProblemDto dto, MultipartFile zipFile) throws IOException {
        log.info("Starting createProblem logic for: {}", dto.getProblemSlug());

        if (problemRepository.existsByProblemSlug(dto.getProblemSlug())) {
            throw new IllegalArgumentException("Problem slug already exists: " + dto.getProblemSlug());
        }

        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Zip file cannot be empty");
        }

        Problem problem = Problem.builder()
                .title(dto.getTitle())
                .subject(dto.getSubject())
                .description(dto.getDescription())
                .timeLimit(dto.getTimeLimit())
                .memoryLimit(dto.getMemoryLimit())
                .hardnessLevel(dto.getHardnessLevel())
                .problemSlug(dto.getProblemSlug())
                .inputDescription(dto.getInputDescription())
                .outputDescription(dto.getOutputDescription())
                .hint(dto.getHint())
                .sampleInput(dto.getSampleInput())
                .sampleOutput(dto.getSampleOutput())
                .status(dto.getStatus().getValue())
                .testCases(new ArrayList<>())
                .build();

        String destDirPath = String.format("%s/%s", AppProperties.TEST_CASE_DIR, dto.getProblemSlug());

        // Extract zip to memory
        log.info("Extracting zip file for problem: {}", dto.getProblemSlug());
        Map<String, byte[]> extractedFiles = FileUtil.extractZip(zipFile);
        log.info("Extracted {} files from zip.", extractedFiles.size());

        // Filter and Match files
        Map<String, byte[]> inputFiles = new HashMap<>();
        Map<String, byte[]> outputFiles = new HashMap<>();

        extractedFiles.forEach((name, content) -> {
            if (name.endsWith(".in")) {
                inputFiles.put(name, content);
            } else if (name.endsWith(".out")) {
                outputFiles.put(name, content);
            }
        });

        log.info("Validating and processing test case files for problem: {}", dto.getProblemSlug());
        inputFiles.forEach((inName, inContent) -> {
            String baseName = inName.substring(0, inName.length() - 3);
            String outName = baseName + ".out";

            if (outputFiles.containsKey(outName)) {
                log.info("Found valid test case pair: {} - {}", inName, outName);
                byte[] outContent = outputFiles.get(outName);

                // Save files
                try {
                    String inputFileName = new java.io.File(inName).getName();
                    String outputFileName = new java.io.File(outName).getName();

                    FileUtil.saveFile(inContent, String.format("%s/%s", destDirPath, inputFileName));
                    FileUtil.saveFile(outContent, String.format("%s/%s", destDirPath, outputFileName));

                    TestCase testCase = TestCase.builder()
                            .input(String.format("%s/%s", dto.getProblemSlug(), inputFileName))
                            .output(String.format("%s/%s", dto.getProblemSlug(), outputFileName))
                            .problem(problem)
                            .build();
                    problem.getTestCases().add(testCase);
                } catch (IOException e) {
                    log.error("Failed to save test case file: {}", inName, e);
                    throw new RuntimeException("Failed to save test case", e);
                }
            }
        });

        // Run async to generate info file
        infoGenerator.generateInfo(dto.getProblemSlug().trim());

        // Sort test cases by name
        problem.getTestCases().sort(Comparator.comparing(TestCase::getInput));

        Problem savedProblem = problemRepository.save(problem);
        log.info("Problem created successfully with ID: {}", savedProblem.getId());
        return problemMapper.toDto(savedProblem);
    }

    public PageResponse<ProblemResponseDto> getProblems(int page, int size, String search, ProblemStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(AppProperties.DEFAULT_SORT_BY).descending());

        Map<String, Object> filters = new HashMap<>();
        if (status != null) {
            filters.put(PropertyUtil.name(Problem::getStatus), status.getValue());
        }

        List<String> searchFields = Arrays.asList(PropertyUtil.name(Problem::getTitle),
                PropertyUtil.name(Problem::getDescription));

        Specification<Problem> spec = new GenericSearchSpecificationUtil<>(filters,
                searchFields,
                search);

        Page<Problem> problemPage = problemRepository.findAll(spec, pageable);
        return PageResponse.of(problemPage.map(problemMapper::toDto));
    }
}
