package vn.thanhtuanle.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtil {

    private HashUtil() {
    }

    /**
     * Lowercase hex of SHA-256(value). Used for the judge_server shared-token handshake:
     * QingdaoU JudgeServer compares the X-Judge-Server-Token header against
     * sha256(TOKEN).hexdigest(), in BOTH directions (backend -> judge_server for /judge,
     * and judge_server -> backend for heartbeats).
     */
    public static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
