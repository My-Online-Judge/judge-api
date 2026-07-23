package vn.thanhtuanle.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import vn.thanhtuanle.problem.dto.CreateProblemDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProblemPackageServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    // parse() needs no repository/service collaborators — pass null for those.
    private final ProblemPackageService service =
            new ProblemPackageService(objectMapper, validator, null, null);

    private static final String VALID_JSON = """
            {"schemaVersion":1,"problem":{
              "title":"A plus B","subject":"math","description":"<p>add</p>",
              "timeLimit":1000,"memoryLimit":256,"hardnessLevel":1,
              "problemSlug":"a-plus-b-import","inputDescription":"i","outputDescription":"o",
              "sampleInput":"1 2","sampleOutput":"3"}}
            """;

    private static byte[] zipOf(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue());
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    private static Map<String, byte[]> validEntries() {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("problem.json", VALID_JSON.getBytes(StandardCharsets.UTF_8));
        entries.put("testcases/1.in", "1 2".getBytes(StandardCharsets.UTF_8));
        entries.put("testcases/1.out", "3".getBytes(StandardCharsets.UTF_8));
        return entries;
    }

    @Test
    void parse_happyPath_returnsDtoAndPairedTestFiles() throws IOException {
        ProblemPackageService.ParsedPackage parsed = service.parse(zipOf(validEntries()));

        CreateProblemDto dto = parsed.dto();
        assertThat(dto.getProblemSlug()).isEqualTo("a-plus-b-import");
        assertThat(dto.getTitle()).isEqualTo("A plus B");
        assertThat(parsed.testFiles()).containsOnlyKeys("1.in", "1.out");
    }

    @Test
    void parse_toleratesSingleWrappingFolder() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        validEntries().forEach((k, v) -> entries.put("a-plus-b/" + k, v));

        ProblemPackageService.ParsedPackage parsed = service.parse(zipOf(entries));
        assertThat(parsed.testFiles()).containsOnlyKeys("1.in", "1.out");
    }

    @Test
    void parse_missingProblemJson_throws() throws IOException {
        Map<String, byte[]> entries = validEntries();
        entries.remove("problem.json");

        assertThatThrownBy(() -> service.parse(zipOf(entries)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("problem.json");
    }

    @Test
    void parse_malformedJson_throws() throws IOException {
        Map<String, byte[]> entries = validEntries();
        entries.put("problem.json", "{not json".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.parse(zipOf(entries)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void parse_unsupportedSchemaVersion_throws() throws IOException {
        Map<String, byte[]> entries = validEntries();
        entries.put("problem.json",
                VALID_JSON.replace("\"schemaVersion\":1", "\"schemaVersion\":2")
                        .getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.parse(zipOf(entries)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schemaVersion");
    }

    @Test
    void parse_constraintViolation_throws() throws IOException {
        Map<String, byte[]> entries = validEntries();
        entries.put("problem.json",
                VALID_JSON.replace("\"title\":\"A plus B\"", "\"title\":\"\"")
                        .getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.parse(zipOf(entries)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title is required");
    }

    @Test
    void parse_noCompletePair_throws() throws IOException {
        Map<String, byte[]> entries = validEntries();
        entries.remove("testcases/1.out");

        assertThatThrownBy(() -> service.parse(zipOf(entries)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test-case pair");
    }

    @Test
    void parse_unsafeEntryName_throws() throws IOException {
        Map<String, byte[]> entries = validEntries();
        entries.put("../evil.sh", "boom".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.parse(zipOf(entries)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsafe");
    }

    @Test
    void parse_notAZip_throws() {
        assertThatThrownBy(() -> service.parse("plain text".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
