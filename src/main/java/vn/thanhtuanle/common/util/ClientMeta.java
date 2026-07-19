package vn.thanhtuanle.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Per-request client telemetry captured at login / token issue. {@code deviceHash} is the
 * client-supplied {@code X-Device-Id} when present, otherwise a hash of the user-agent so a
 * value is always available. All fields are sized to their DB columns.
 */
public record ClientMeta(String ip, String deviceHash, String userAgent) {

    private static final int UA_MAX = 512;
    private static final int DEVICE_MAX = 128;

    public static ClientMeta from(HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        String userAgent = truncate(request.getHeader("User-Agent"), UA_MAX);

        String deviceId = request.getHeader("X-Device-Id");
        String deviceHash = (deviceId != null && !deviceId.isBlank())
                ? truncate(deviceId.trim(), DEVICE_MAX)
                : (userAgent != null ? truncate(sha256Hex(userAgent), DEVICE_MAX) : null);

        return new ClientMeta(ip, deviceHash, userAgent);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
