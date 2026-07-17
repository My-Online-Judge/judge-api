package vn.thanhtuanle.common.util;

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

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", ACCESS_TTL);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 259_200_000L);
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
