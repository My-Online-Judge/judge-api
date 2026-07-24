package vn.thanhtuanle.common.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.thanhtuanle.entity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T4-9b: all new tokens are RS256 (+kid); verification accepts RS256 always and
 * HS256 only while the legacy secret is still configured (time-boxed dual-verify).
 */
class JwtUtilRs256Test {

    // A fixed legacy HS256 secret (base64 of 64 random bytes), matching the old format.
    private static final String LEGACY_SECRET =
            Base64.getEncoder().encodeToString(
                    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    private static KeyPair rsaPair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    /** base64( PEM armor around base64(DER) ) — the exact env-var format the design mandates. */
    private static String pemBase64(String type, byte[] der) {
        String pem = "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der)
                + "\n-----END " + type + "-----\n";
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    private static JwtUtil newJwtUtil(KeyPair pair, String legacySecret) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "rsaPrivateKeyPem", pemBase64("PRIVATE KEY", pair.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(util, "rsaPublicKeyPem", pemBase64("PUBLIC KEY", pair.getPublic().getEncoded()));
        ReflectionTestUtils.setField(util, "secretKey", legacySecret == null ? "" : legacySecret);
        ReflectionTestUtils.setField(util, "jwtExpiration", 900000L);
        ReflectionTestUtils.setField(util, "refreshExpiration", 259200000L);
        util.init();
        return util;
    }

    private static User alice() {
        return User.builder().username("alice@example.com").build();
    }

    /** Decode the JWS header (first dot-segment) as a UTF-8 JSON string. */
    private static String headerJson(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    }

    @Test
    void rs256TokenRoundTrips() throws Exception {
        JwtUtil util = newJwtUtil(rsaPair(), null);
        String token = util.generateToken(alice());
        assertThat(util.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(util.extractJti(token)).isNotBlank();
    }

    @Test
    void newTokensCarryRs256AndKidHeader() throws Exception {
        KeyPair pair = rsaPair();
        JwtUtil util = newJwtUtil(pair, null);
        String header = headerJson(util.generateToken(alice()));

        byte[] digest = MessageDigest.getInstance("SHA-256").digest(pair.getPublic().getEncoded());
        String expectedKid = HexFormat.of().formatHex(digest).substring(0, 16);

        assertThat(header).contains("\"alg\":\"RS256\"");
        assertThat(header).contains("\"kid\":\"" + expectedKid + "\"");
        assertThat(util.getKeyId()).isEqualTo(expectedKid);
    }

    @Test
    void refreshTokensAreAlsoRs256() throws Exception {
        JwtUtil util = newJwtUtil(rsaPair(), null);
        assertThat(headerJson(util.generateRefreshToken(alice()))).contains("\"alg\":\"RS256\"");
    }

    @Test
    void legacyHs256TokenVerifies_whileSecretConfigured() throws Exception {
        JwtUtil util = newJwtUtil(rsaPair(), LEGACY_SECRET);
        String legacyToken = Jwts.builder()
                .setSubject("alice@example.com")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(LEGACY_SECRET)), SignatureAlgorithm.HS256)
                .compact();
        assertThat(util.extractUsername(legacyToken)).isEqualTo("alice@example.com");
    }

    @Test
    void legacyHs256TokenRejected_whenSecretAbsent() throws Exception {
        JwtUtil util = newJwtUtil(rsaPair(), null); // fallback disabled
        String legacyToken = Jwts.builder()
                .setSubject("alice@example.com")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(LEGACY_SECRET)), SignatureAlgorithm.HS256)
                .compact();
        assertThatThrownBy(() -> util.extractUsername(legacyToken))
                .isInstanceOf(UnsupportedJwtException.class);
    }

    @Test
    void tokenSignedByDifferentRsaKeyRejected() throws Exception {
        JwtUtil trusted = newJwtUtil(rsaPair(), null);
        JwtUtil attacker = newJwtUtil(rsaPair(), null);
        String forged = attacker.generateToken(alice());
        assertThatThrownBy(() -> trusted.extractUsername(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void unsignedAlgNoneTokenRejected() throws Exception {
        JwtUtil util = newJwtUtil(rsaPair(), LEGACY_SECRET);
        String unsigned = Jwts.builder().setSubject("alice@example.com").compact(); // alg=none
        assertThatThrownBy(() -> util.extractUsername(unsigned))
                .isInstanceOf(UnsupportedJwtException.class);
    }

    @Test
    void algConfusion_hs256SignedWithPublicKeyBytes_rejected() throws Exception {
        KeyPair pair = rsaPair();
        JwtUtil util = newJwtUtil(pair, LEGACY_SECRET);
        // Classic key-confusion attack: use the PUBLIC key bytes as an HMAC secret.
        String forged = Jwts.builder()
                .setSubject("alice@example.com")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(pair.getPublic().getEncoded()), SignatureAlgorithm.HS256)
                .compact();
        // HS256 resolves to the LEGACY secret (never the public key), so the signature can't match.
        assertThatThrownBy(() -> util.extractUsername(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void missingOrGarbageRsaKeysFailFast() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "rsaPrivateKeyPem", "bm90LWEta2V5");
        ReflectionTestUtils.setField(util, "rsaPublicKeyPem", "bm90LWEta2V5");
        ReflectionTestUtils.setField(util, "secretKey", "");
        assertThatThrownBy(util::init).isInstanceOf(IllegalStateException.class);
    }
}
