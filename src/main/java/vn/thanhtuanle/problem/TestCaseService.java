package vn.thanhtuanle.problem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.common.util.FileUtil;
import vn.thanhtuanle.common.util.GenerateTestCaseInfoUtil;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.problem.dto.TestCaseResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Manages a problem's test cases after creation (list / add / import / delete). Files live under
 * {@code TEST_CASE_DIR/{slug}/} and are named with the next integer index so the judge (which
 * sorts {@code .in} files numerically) keeps a stable order. After every mutation the problem's
 * info file is regenerated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final GenerateTestCaseInfoUtil infoGenerator;

    private static final Comparator<TestCaseResponse> BY_NUMERIC_NAME =
            Comparator.comparingInt(r -> parseIntOrMax(r.getName()));

    @Transactional(readOnly = true)
    public List<TestCaseResponse> listTestCases(String slug) {
        log.info("Start list test cases for problem: {}", slug);
        Problem problem = getProblemOrThrow(slug);

        List<TestCaseResponse> result = new ArrayList<>();
        for (TestCase tc : problem.getTestCases()) {
            result.add(TestCaseResponse.builder()
                    .id(tc.getId())
                    .name(baseNameOf(tc.getInput()))
                    .input(readContentQuietly(tc.getInput()))
                    .output(readContentQuietly(tc.getOutput()))
                    .build());
        }
        result.sort(BY_NUMERIC_NAME);
        log.info("End list test cases for problem: {} (count={})", slug, result.size());
        return result;
    }

    @Transactional
    public TestCaseResponse addTestCase(String slug, MultipartFile input, MultipartFile output) throws IOException {
        log.info("Start add test case for problem: {}", slug);
        validateFile(input, "input");
        validateFile(output, "output");

        Problem problem = getProblemOrThrow(slug);
        int index = nextIndexFor(problem);

        byte[] inBytes = input.getBytes();
        byte[] outBytes = output.getBytes();
        TestCase created = persistTestCase(problem, slug, index, inBytes, outBytes);

        problemRepository.save(problem);
        infoGenerator.generateInfo(slug);

        log.info("End add test case for problem: {} as index {}", slug, index);
        return toResponse(created, inBytes, outBytes);
    }

    @Transactional
    public List<TestCaseResponse> importTestCases(String slug, MultipartFile zipFile) throws IOException {
        log.info("Start import test cases for problem: {}", slug);
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Zip file cannot be empty");
        }

        Problem problem = getProblemOrThrow(slug);

        Map<String, byte[]> extracted = FileUtil.extractZip(zipFile);
        Map<String, byte[]> inputFiles = new HashMap<>();
        Map<String, byte[]> outputFiles = new HashMap<>();
        extracted.forEach((name, content) -> {
            if (name.endsWith(AppProperties.INPUT_FILE_EXTENSION)) {
                inputFiles.put(name, content);
            } else if (name.endsWith(AppProperties.OUTPUT_FILE_EXTENSION)) {
                outputFiles.put(name, content);
            }
        });

        // Keep only inputs that have a matching output, re-numbered deterministically.
        List<String> pairedInputNames = inputFiles.keySet().stream()
                .filter(inName -> outputFiles.containsKey(outputNameFor(inName)))
                .sorted(numericAwareNameComparator())
                .toList();

        int index = nextIndexFor(problem);
        List<TestCase> newCases = new ArrayList<>();
        List<byte[]> newInputs = new ArrayList<>();
        List<byte[]> newOutputs = new ArrayList<>();

        for (String inName : pairedInputNames) {
            byte[] inBytes = inputFiles.get(inName);
            byte[] outBytes = outputFiles.get(outputNameFor(inName));
            newCases.add(persistTestCase(problem, slug, index, inBytes, outBytes));
            newInputs.add(inBytes);
            newOutputs.add(outBytes);
            index++;
        }

        problemRepository.save(problem);
        infoGenerator.generateInfo(slug);

        List<TestCaseResponse> responses = new ArrayList<>();
        for (int i = 0; i < newCases.size(); i++) {
            responses.add(toResponse(newCases.get(i), newInputs.get(i), newOutputs.get(i)));
        }
        log.info("End import test cases for problem: {} (added={})", slug, responses.size());
        return responses;
    }

    @Transactional
    public void deleteTestCase(String slug, UUID testCaseId) {
        log.info("Start delete test case {} for problem: {}", testCaseId, slug);
        TestCase tc = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found: " + testCaseId));

        // Ownership check: the test case must belong to the {slug} problem.
        if (tc.getProblem() == null || !slug.equals(tc.getProblem().getProblemSlug())) {
            throw new ResourceNotFoundException(
                    "Test case " + testCaseId + " not found for problem " + slug);
        }

        deleteFileQuietly(tc.getInput());
        deleteFileQuietly(tc.getOutput());
        testCaseRepository.delete(tc);
        infoGenerator.generateInfo(slug);
        log.info("End delete test case {} for problem: {}", testCaseId, slug);
    }

    // --- persistence / IO helpers -------------------------------------------------------------

    private TestCase persistTestCase(Problem problem, String slug, int index, byte[] inBytes, byte[] outBytes)
            throws IOException {
        String inRelative = relativePath(slug, index, AppProperties.INPUT_FILE_EXTENSION);
        String outRelative = relativePath(slug, index, AppProperties.OUTPUT_FILE_EXTENSION);

        FileUtil.saveFile(inBytes, String.format("%s/%s", AppProperties.TEST_CASE_DIR, inRelative));
        FileUtil.saveFile(outBytes, String.format("%s/%s", AppProperties.TEST_CASE_DIR, outRelative));

        TestCase testCase = TestCase.builder()
                .input(inRelative)
                .output(outRelative)
                .problem(problem)
                .build();
        problem.getTestCases().add(testCase);
        return testCase;
    }

    private Problem getProblemOrThrow(String slug) {
        return problemRepository.findByProblemSlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with slug: " + slug));
    }

    private int nextIndexFor(Problem problem) {
        List<Integer> existing = problem.getTestCases().stream()
                .map(tc -> baseIndexOf(tc.getInput()))
                .filter(Objects::nonNull)
                .toList();
        return nextIndex(existing);
    }

    private String readContentQuietly(String relativePath) {
        try {
            return Files.readString(Paths.get(AppProperties.TEST_CASE_DIR, relativePath));
        } catch (IOException e) {
            log.warn("Test case file not readable, returning empty content: {}", relativePath);
            return "";
        }
    }

    private void deleteFileQuietly(String relativePath) {
        try {
            Files.deleteIfExists(Paths.get(AppProperties.TEST_CASE_DIR, relativePath));
        } catch (IOException e) {
            log.warn("Failed to delete test case file: {}", relativePath, e);
        }
    }

    private static void validateFile(MultipartFile file, String field) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(field + " file cannot be empty");
        }
    }

    private static TestCaseResponse toResponse(TestCase tc, byte[] inBytes, byte[] outBytes) {
        return TestCaseResponse.builder()
                .id(tc.getId())
                .name(baseNameOf(tc.getInput()))
                .input(new String(inBytes, StandardCharsets.UTF_8))
                .output(new String(outBytes, StandardCharsets.UTF_8))
                .build();
    }

    private static String relativePath(String slug, int index, String extension) {
        return String.format("%s/%d%s", slug, index, extension);
    }

    private static String outputNameFor(String inputName) {
        return inputName.substring(0, inputName.length() - AppProperties.INPUT_FILE_EXTENSION.length())
                + AppProperties.OUTPUT_FILE_EXTENSION;
    }

    private static Comparator<String> numericAwareNameComparator() {
        return Comparator.<String>comparingInt(TestCaseService::parseIntOrMax)
                .thenComparing(Comparator.naturalOrder());
    }

    // --- pure helpers (unit-tested) -----------------------------------------------------------

    /**
     * Next 1-based test case index: {@code max(existingIndices) + 1}, or {@code 1} when empty.
     */
    static int nextIndex(Collection<Integer> existingIndices) {
        return existingIndices.stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    /**
     * Numeric base name of a path/file name, or {@code null} when it is not a plain integer.
     * e.g. {@code "a-plus-b/12.in"} -> {@code 12}, {@code "a-plus-b/sample.in"} -> {@code null}.
     */
    static Integer baseIndexOf(String pathOrName) {
        try {
            return Integer.parseInt(baseNameOf(pathOrName).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * File name without directory or extension. {@code "a-plus-b/12.in"} -> {@code "12"}.
     */
    static String baseNameOf(String pathOrName) {
        if (pathOrName == null) {
            return "";
        }
        String name = pathOrName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(0, dot);
        }
        return name;
    }

    private static int parseIntOrMax(String pathOrName) {
        Integer idx = baseIndexOf(pathOrName);
        return idx == null ? Integer.MAX_VALUE : idx;
    }
}
