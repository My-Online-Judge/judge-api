package vn.thanhtuanle.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Component
public class GenerateTestCaseInfoUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_CASES_BASE_DIR = Paths.get("src", "main", "resources", "test_cases").toString();

    /**
     * Generate info file for a problem based on the test cases directory
     * 
     * @param problemSlug Slug of problem
     */

    @Async
    public void generateInfo(String problemSlug) {
        if (!StringUtils.hasText(problemSlug)) {
            log.warn("Problem slug is empty, skipping generate info");
            return;
        }

        Path testCaseDir = Paths.get(TEST_CASES_BASE_DIR, problemSlug).toAbsolutePath().normalize();

        if (!Files.exists(testCaseDir) || !Files.isDirectory(testCaseDir)) {
            log.error("Directory not found or not a directory: {}", testCaseDir);
            return;
        }

        try {
            Map<String, Map<String, Object>> testCases = new TreeMap<>(); // TreeMap để sort theo key

            // Get all .in files
            List<Path> inFiles = Files.list(testCaseDir)
                    .filter(p -> p.toString().endsWith(".in"))
                    .sorted(Comparator.comparing(p -> {
                        String name = p.getFileName().toString();
                        String numStr = name.replace(".in", "");
                        try {
                            return Integer.parseInt(numStr);
                        } catch (NumberFormatException e) {
                            return Integer.MAX_VALUE; // fallback
                        }
                    }))
                    .toList();

            for (Path inPath : inFiles) {
                String name = inPath.getFileName().toString().replace(".in", "");
                Path outPath = testCaseDir.resolve(name + ".out");

                if (!Files.exists(outPath)) {
                    log.warn("Output file not found: {} for input {}", outPath, inPath);
                    continue;
                }

                byte[] inputData = Files.readAllBytes(inPath);
                byte[] outputData = Files.readAllBytes(outPath);

                String outputStr;
                try {
                    outputStr = new String(outputData, StandardCharsets.UTF_8).replaceAll("\\r?\\n$", ""); // rstrip
                } catch (Exception e) {
                    // fallback binary
                    outputStr = new String(outputData);
                }

                String strippedMd5 = md5Hex(outputStr.getBytes(StandardCharsets.UTF_8));
                String fullMd5 = md5Hex(outputData);

                Map<String, Object> tc = new LinkedHashMap<>();
                tc.put("input_name", inPath.getFileName().toString());
                tc.put("input_size", inputData.length);
                tc.put("output_name", outPath.getFileName().toString());
                tc.put("output_size", outputData.length);
                tc.put("output_md5", fullMd5);
                tc.put("stripped_output_md5", strippedMd5);

                testCases.put(name, tc);
            }

            if (testCases.isEmpty()) {
                log.warn("No valid test cases found in {}", testCaseDir);
                return;
            }

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("test_case_number", testCases.size());
            info.put("spj", false);
            info.put("test_cases", testCases);

            Path infoPath = testCaseDir.resolve("info");
            Files.writeString(infoPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(info));

            log.info("Generated info file successfully at: {}", infoPath);

        } catch (IOException e) {
            log.error("Failed to generate info for problem {}: {}", problemSlug, e.getMessage(), e);
        }
    }

    private String md5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}