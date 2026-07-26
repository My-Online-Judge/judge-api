package vn.thanhtuanle.common.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.thanhtuanle.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    // 48 zero-bytes, base64-encoded — JwtUtil BASE64-decodes the secret and HS256 needs >= 32 bytes.
    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final long ACCESS_TTL = 900_000L; // 15 min

    private JwtUtil jwtUtil;

    /** base64( PEM armor around base64(DER) ) — the format JwtUtil.init() expects (T4-9b, RS256). */
    private static String pemBase64(String type, byte[] der) {
        String pem = "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der)
                + "\n-----END " + type + "-----\n";
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        // T4-9b: JwtUtil now signs with RS256, so RSA keys must be present and init() run
        // even though this test only cares about the HS256-era jti/TTL behaviors below.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "rsaPrivateKeyPem", pemBase64("PRIVATE KEY", pair.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(jwtUtil, "rsaPublicKeyPem", pemBase64("PUBLIC KEY", pair.getPublic().getEncoded()));
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", ACCESS_TTL);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 259_200_000L);
        jwtUtil.init();
    }

    private User user() {
        return User.builder().username("alice@example.com").build();
    }

    @Test
    void generatedToken_carriesAJti() {
        String token = jwtUtil.generateToken(user());

        String jti = jwtUtil.extractJti(token);

        assertThat(jti).isNotBlank();
    }

    @Test
    void eachToken_getsAUniqueJti() {
        String jtiA = jwtUtil.extractJti(jwtUtil.generateToken(user()));
        String jtiB = jwtUtil.extractJti(jwtUtil.generateToken(user()));

        assertThat(jtiA).isNotEqualTo(jtiB);
    }

    @Test
    void remainingTtl_isCloseToTheConfiguredExpiration() {
        String token = jwtUtil.generateToken(user());

        long remaining = jwtUtil.getRemainingTtlMillis(token);

        // Freshly minted: within a small margin below the full 15-minute TTL.
        assertThat(remaining).isBetween(ACCESS_TTL - 5_000L, ACCESS_TTL);
    }
}
