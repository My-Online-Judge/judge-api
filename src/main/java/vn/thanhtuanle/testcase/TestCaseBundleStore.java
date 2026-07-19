package vn.thanhtuanle.testcase;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.thanhtuanle.common.constant.AppProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Publishes a problem's test cases to MinIO as an immutable, content-addressed bundle and resolves
 * the "current" bundle for judging. Bundle object: {@code <slug>/<hash>.zip}; pointer object:
 * {@code <slug>/CURRENT} (plain-text hash). Because a (slug, hash) bundle never changes, downstream
 * caches never go stale.
 */
@Component
@Slf4j
public class TestCaseBundleStore {

    private final MinioClient minio;
    private final MinioProperties props;
    private final Path baseDir;

    @Autowired
    public TestCaseBundleStore(MinioClient minio, MinioProperties props) {
        this(minio, props, Paths.get(AppProperties.TEST_CASE_DIR));
    }

    // package-private: tests inject a temp base dir.
    TestCaseBundleStore(MinioClient minio, MinioProperties props, Path baseDir) {
        this.minio = minio;
        this.props = props;
        this.baseDir = baseDir;
    }

    public String publish(String slug) {
        try {
            List<Path> files = listBundleFiles(baseDir.resolve(slug));
            if (files.isEmpty()) {
                throw new TestCaseBundleException("No test-case files to publish for problem: " + slug);
            }
            String hash = contentHash(files);
            String key = bundleKey(slug, hash);
            if (!objectExists(key)) {
                putBytes(key, zip(files), "application/zip");
            }
            putBytes(currentKey(slug), hash.getBytes(StandardCharsets.UTF_8), "text/plain");
            log.info("Published test-case bundle for {} -> {}", slug, hash);
            return hash;
        } catch (TestCaseBundleException e) {
            throw e;
        } catch (Exception e) {
            throw new TestCaseBundleException("Failed to publish bundle for problem: " + slug, e);
        }
    }

    public String currentVersion(String slug) {
        try (InputStream is = minio.getObject(GetObjectArgs.builder()
                .bucket(props.getBucket()).object(currentKey(slug)).build())) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new TestCaseBundleException("No published test-case bundle for problem: " + slug, e);
        }
    }

    public boolean hasBundle(String slug) {
        return objectExists(currentKey(slug));
    }

    public void ensureBucket() {
        try {
            boolean exists = minio.bucketExists(
                    BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                minio.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                log.info("Created MinIO bucket {}", props.getBucket());
            }
        } catch (Exception e) {
            throw new TestCaseBundleException("Failed to ensure bucket " + props.getBucket(), e);
        }
    }

    private boolean objectExists(String key) {
        try {
            minio.statObject(StatObjectArgs.builder().bucket(props.getBucket()).object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            String code = e.errorResponse() != null ? e.errorResponse().code() : null;
            if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code)) {
                return false;  // genuinely absent
            }
            // 403 AccessDenied, throttling, etc. must NOT read as "absent".
            throw new TestCaseBundleException("Failed to stat object " + key + " (code=" + code + ")", e);
        } catch (Exception e) {
            throw new TestCaseBundleException("Failed to stat object " + key, e);
        }
    }

    private void putBytes(String key, byte[] data, String contentType) throws Exception {
        try (InputStream is = new ByteArrayInputStream(data)) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket()).object(key)
                    .stream(is, data.length, -1)
                    .contentType(contentType)
                    .build());
        }
    }

    static List<Path> listBundleFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    static String contentHash(List<Path> sortedFiles) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (Path f : sortedFiles) {
            md.update(f.getFileName().toString().getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(Files.readAllBytes(f));
        }
        return HexFormat.of().formatHex(md.digest()).substring(0, 12);
    }

    static byte[] zip(List<Path> sortedFiles) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Path f : sortedFiles) {
                ZipEntry entry = new ZipEntry(f.getFileName().toString());
                entry.setTime(0L); // deterministic archive
                zos.putNextEntry(entry);
                zos.write(Files.readAllBytes(f));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    static String bundleKey(String slug, String hash) {
        return slug + "/" + hash + ".zip";
    }

    static String currentKey(String slug) {
        return slug + "/CURRENT";
    }
}
