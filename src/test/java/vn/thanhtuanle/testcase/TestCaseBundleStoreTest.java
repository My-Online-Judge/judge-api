package vn.thanhtuanle.testcase;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseBundleStoreTest {

    @Mock
    MinioClient minio;

    private MinioProperties props() {
        MinioProperties p = new MinioProperties();
        p.setBucket("test-cases");
        return p;
    }

    private Path problemDir(Path base, String slug) throws Exception {
        Path dir = base.resolve(slug);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("1.in"), "1 2\n");
        Files.writeString(dir.resolve("1.out"), "3\n");
        Files.writeString(dir.resolve("info"), "{\"test_case_number\":1}");
        return dir;
    }

    @Test
    void contentHash_isStableAndTwelveHex(@TempDir Path base) throws Exception {
        Path dir = problemDir(base, "p");
        String h1 = TestCaseBundleStore.contentHash(TestCaseBundleStore.listBundleFiles(dir));
        String h2 = TestCaseBundleStore.contentHash(TestCaseBundleStore.listBundleFiles(dir));
        assertThat(h1).isEqualTo(h2).hasSize(12).matches("[0-9a-f]{12}");
    }

    @Test
    void publish_uploadsBundleAndPointsCurrentAtTheHash(@TempDir Path base) throws Exception {
        problemDir(base, "p");
        // statObject throws NoSuchKey -> object absent -> bundle gets uploaded.
        when(minio.statObject(any(StatObjectArgs.class)))
                .thenThrow(noSuchKey());
        TestCaseBundleStore store = new TestCaseBundleStore(minio, props(), base);

        String hash = store.publish("p");

        ArgumentCaptor<PutObjectArgs> puts = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio, org.mockito.Mockito.times(2)).putObject(puts.capture());
        List<String> objects = puts.getAllValues().stream().map(PutObjectArgs::object).toList();
        assertThat(objects).containsExactlyInAnyOrder("p/" + hash + ".zip", "p/CURRENT");
    }

    @Test
    void publish_skipsUploadWhenBundleAlreadyExists(@TempDir Path base) throws Exception {
        problemDir(base, "p");
        when(minio.statObject(any(StatObjectArgs.class))).thenReturn(null); // present
        TestCaseBundleStore store = new TestCaseBundleStore(minio, props(), base);

        store.publish("p");

        // Only CURRENT is (re)written; the immutable bundle upload is skipped.
        ArgumentCaptor<PutObjectArgs> puts = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio).putObject(puts.capture());
        assertThat(puts.getValue().object()).isEqualTo("p/CURRENT");
    }

    @Test
    void publish_throwsWhenNoTestCaseFiles(@TempDir Path base) throws Exception {
        Files.createDirectories(base.resolve("empty"));
        TestCaseBundleStore store = new TestCaseBundleStore(minio, props(), base);
        assertThatThrownBy(() -> store.publish("empty"))
                .isInstanceOf(TestCaseBundleException.class);
        verify(minio, never()).putObject(any());
    }

    @Test
    void currentVersion_readsThePointerObject(@TempDir Path base) throws Exception {
        GetObjectResponse resp = new GetObjectResponse(
                Headers.of(), "test-cases", null, "p/CURRENT",
                new ByteArrayInputStream("deadbeef1234".getBytes(StandardCharsets.UTF_8)));
        when(minio.getObject(any(GetObjectArgs.class))).thenReturn(resp);
        TestCaseBundleStore store = new TestCaseBundleStore(minio, props(), base);

        assertThat(store.currentVersion("p")).isEqualTo("deadbeef1234");
    }

    @Test
    void currentVersion_throwsWhenAbsent(@TempDir Path base) throws Exception {
        when(minio.getObject(any(GetObjectArgs.class))).thenThrow(noSuchKey());
        TestCaseBundleStore store = new TestCaseBundleStore(minio, props(), base);
        assertThatThrownBy(() -> store.currentVersion("p"))
                .isInstanceOf(TestCaseBundleException.class);
    }

    private static ErrorResponseException noSuchKey() throws Exception {
        return new ErrorResponseException(
                new io.minio.messages.ErrorResponse(
                        "NoSuchKey", "not found", "test-cases", "k", "/k", "req", "host"),
                new okhttp3.Response.Builder()
                        .request(new okhttp3.Request.Builder().url("http://localhost:9000/k").build())
                        .protocol(okhttp3.Protocol.HTTP_1_1).code(404).message("Not Found").build(),
                "trace");
    }
}
