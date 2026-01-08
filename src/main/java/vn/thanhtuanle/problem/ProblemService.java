package vn.thanhtuanle.problem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.payload.PageResponse;
import vn.thanhtuanle.common.enums.ProblemStatus;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.common.exception.ResourceAlreadyExistException;
import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.common.util.FileUtil;
import vn.thanhtuanle.common.util.GenerateTestCaseInfoUtil;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.problem.dto.CreateProblemDto;
import vn.thanhtuanle.problem.dto.ProblemResponseDto;
import vn.thanhtuanle.problem.dto.ProblemStatisticProjection;
import vn.thanhtuanle.problem.dto.ProblemStatisticsInfo;
import vn.thanhtuanle.problem.mapper.ProblemMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

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

        validateCreateRequest(dto, zipFile);

        Problem problem = problemMapper.toEntity(dto);
        problem.setTestCases(new ArrayList<>());

        String destDirPath = String.format("%s/%s", AppProperties.TEST_CASE_DIR, dto.getProblemSlug());

        // Process files
        processTestCases(zipFile, problem, destDirPath);

        // Run async to generate info file
        infoGenerator.generateInfo(dto.getProblemSlug().trim());

        // Sort test cases by name
        problem.getTestCases().sort(Comparator.comparing(TestCase::getInput));

        Problem savedProblem = problemRepository.save(problem);
        log.info("Problem created successfully with ID: {}", savedProblem.getId());
        return problemMapper.toDto(savedProblem);
    }

    private void validateCreateRequest(CreateProblemDto dto, MultipartFile zipFile) {
        if (problemRepository.existsByProblemSlug(dto.getProblemSlug())) {
            throw new ResourceAlreadyExistException("Problem slug already exists: " + dto.getProblemSlug());
        }

        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Zip file cannot be empty");
        }
    }

    private void processTestCases(MultipartFile zipFile, Problem problem, String destDirPath) throws IOException {
        // Extract zip to memory
        log.info("Extracting zip file for problem: {}", problem.getProblemSlug());
        Map<String, byte[]> extractedFiles = FileUtil.extractZip(zipFile);
        log.info("Extracted {} files from zip.", extractedFiles.size());

        // Filter files
        Map<String, byte[]> inputFiles = new HashMap<>();
        Map<String, byte[]> outputFiles = new HashMap<>();

        extractedFiles.forEach((name, content) -> {
            if (name.endsWith(AppProperties.INPUT_FILE_EXTENSION)) {
                inputFiles.put(name, content);
            } else if (name.endsWith(AppProperties.OUTPUT_FILE_EXTENSION)) {
                outputFiles.put(name, content);
            }
        });

        log.info("Validating and processing test case files for problem: {}", problem.getProblemSlug());
        matchAndSaveTestCases(inputFiles, outputFiles, problem, destDirPath);
    }

    private void matchAndSaveTestCases(Map<String, byte[]> inputFiles, Map<String, byte[]> outputFiles, Problem problem,
            String destDirPath) {
        inputFiles.forEach((inName, inContent) -> {
            String baseName = inName.substring(0, inName.length() - AppProperties.INPUT_FILE_EXTENSION.length());
            String outName = baseName + AppProperties.OUTPUT_FILE_EXTENSION;

            if (outputFiles.containsKey(outName)) {
                log.info("Found valid test case pair: {} - {}", inName, outName);
                byte[] outContent = outputFiles.get(outName);

                try {
                    String inputFileName = new File(inName).getName();
                    String outputFileName = new File(outName).getName();

                    FileUtil.saveFile(inContent, String.format("%s/%s", destDirPath, inputFileName));
                    FileUtil.saveFile(outContent, String.format("%s/%s", destDirPath, outputFileName));

                    TestCase testCase = TestCase.builder()
                            .input(String.format("%s/%s", problem.getProblemSlug(), inputFileName))
                            .output(String.format("%s/%s", problem.getProblemSlug(), outputFileName))
                            .problem(problem)
                            .build();
                    problem.getTestCases().add(testCase);
                } catch (IOException e) {
                    log.error("Failed to save test case file: {}", inName, e);
                    throw new RuntimeException("Failed to save test case", e);
                }
            }
        });
    }

    public PageResponse<ProblemResponseDto> getProblems(int page, int size, String search, ProblemStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(AppProperties.DEFAULT_SORT_BY).descending());

        Integer statusValue = status != null ? status.getValue() : null;
        Page<ProblemStatisticProjection> problemPage = problemRepository.findProblemsWithStats(search, statusValue,
                pageable);
        return PageResponse.of(problemPage.map(problemMapper::toDto));
    }

    public ProblemResponseDto getProblemBySlug(String slug) {
        log.info("Start fetch problem details for slug: {}", slug);
        ProblemStatisticProjection projection = problemRepository.findByProblemSlugWithStats(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found with slug: " + slug));

        ProblemResponseDto dto = problemMapper.toDto(projection);

        List<ProblemStatisticsInfo> rawStats = problemRepository.countSubmissionsByResult(projection.getId());

        log.info("Initializing submission result statistics for problem slug: {}", slug);
        Map<String, Integer> statisticInfo = Arrays.stream(SubmissionResult.values())
                .collect(Collectors.toMap(
                        result -> String.valueOf(result.getValue()),
                        result -> 0));

        log.info("Updating submission result statistics with actual counts for problem slug: {}", slug);
        rawStats.forEach(stat -> statisticInfo.put(
                String.valueOf(stat.getResult()),
                stat.getCount().intValue()));

        dto.setStatisticInfo(statisticInfo);

        log.info("End fetch problem details for slug: {}", slug);
        return dto;
    }
}
