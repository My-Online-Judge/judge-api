package vn.thanhtuanle.common.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {

    public static void saveFile(byte[] data, String destPath) throws IOException {
        Path path = Paths.get(destPath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            fos.write(data);
        }
    }

    public static Map<String, byte[]> extractZip(MultipartFile zipFile) throws IOException {
        Map<String, byte[]> extractedFiles = new HashMap<>();
        try (InputStream fis = zipFile.getInputStream();
                ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                    extractedFiles.put(zipEntry.getName(), bos.toByteArray());
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
        return extractedFiles;
    }

    /**
     * Recursively delete a directory and its contents. Best-effort: a missing directory is a
     * no-op, and individual delete failures bubble up as an unchecked exception.
     */
    public static void deleteDirectoryQuietly(String dirPath) {
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
