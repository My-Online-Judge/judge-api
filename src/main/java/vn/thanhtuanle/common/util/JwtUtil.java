package vn.thanhtuanle.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.thanhtuanle.entity.Role;
import vn.thanhtuanle.entity.User;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtUtil {

    /**
     * Legacy HS256 secret. Optional: while present, old HS256 tokens still verify
     * (time-boxed dual-verify); once the env var is removed, HS256 is rejected.
     */
    @Value("${application.security.jwt.secret-key:}")
    private String secretKey;

    /** base64( PEM ) of the PKCS#8 RSA private key — signs every new token (RS256). */
    @Value("${application.security.jwt.rsa.private-key}")
    private String rsaPrivateKeyPem;

    /** base64( PEM ) of the X.509 RSA public key — verifies RS256 tokens. */
    @Value("${application.security.jwt.rsa.public-key}")
    private String rsaPublicKeyPem;

    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;
    private String keyId;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @PostConstruct
    void init() {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            rsaPrivateKey = (RSAPrivateKey) kf.generatePrivate(
                    new PKCS8EncodedKeySpec(decodePem(rsaPrivateKeyPem, "PRIVATE KEY")));
            rsaPublicKey = (RSAPublicKey) kf.generatePublic(
                    new X509EncodedKeySpec(decodePem(rsaPublicKeyPem, "PUBLIC KEY")));
            byte[] fingerprint = MessageDigest.getInstance("SHA-256").digest(rsaPublicKey.getEncoded());
            keyId = HexFormat.of().formatHex(fingerprint).substring(0, 16);
        } catch (Exception e) {
            // Fail fast: a judge-api that cannot sign or verify tokens must not boot.
            throw new IllegalStateException(
                    "Invalid or missing RSA JWT keys (JWT_RSA_PRIVATE_KEY / JWT_RSA_PUBLIC_KEY)", e);
        }
    }

    /** kid stamped into every token header — first 16 hex chars of SHA-256(publicKey DER). */
    public String getKeyId() {
        return keyId;
    }

    /** env value = base64(PEM file); strip the armor, decode the DER body. */
    private static byte[] decodePem(String base64Pem, String type) {
        String pem = new String(java.util.Base64.getDecoder().decode(base64Pem), StandardCharsets.UTF_8);
        String body = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return java.util.Base64.getDecoder().decode(body);
    }

    public String extractUsername(String jwtToken) {
        return extractClaims(jwtToken, Claims::getSubject);
    }

    /** The token's unique id (jti) — the key under which a revoked token is tracked in Redis. */
    public String extractJti(String jwtToken) {
        return extractClaims(jwtToken, Claims::getId);
    }

    /**
     * Milliseconds left until this token expires, clamped to 0. Used as the TTL when a token's
     * jti is parked in Redis so the entry self-evicts exactly when the token would have expired.
     */
    public long getRemainingTtlMillis(String jwtToken) {
        try {
            return Math.max(0, extractExpiration(jwtToken).getTime() - System.currentTimeMillis());
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }

    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(User userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extractClaims, User userDetails) {
        // Add roles to claims
        if (userDetails.getRoles() != null) {
            extractClaims.put("roles", userDetails.getRoles().stream()
                    .map(Role::getName)
                    .toList());
        }
        return buildToken(extractClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(User userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    private String buildToken(Map<String, Object> extractClaims, User user, long expiration) {
        return Jwts
                .builder()
                .setHeaderParam(JwsHeader.KEY_ID, keyId)
                .setClaims(extractClaims)
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(rsaPrivateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractUsername(token);
            return (email.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKeyResolver(signingKeyResolver)
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Two-entry allowlist keyed by the token's declared alg. The RSA public key is never
     * offered to the HMAC path, so the classic RS256->HS256 key-confusion attack cannot
     * type-check, and jjwt additionally enforces key/alg agreement.
     */
    private final SigningKeyResolver signingKeyResolver = new SigningKeyResolverAdapter() {
        @Override
        public Key resolveSigningKey(JwsHeader header, Claims claims) {
            String alg = header.getAlgorithm();
            if (SignatureAlgorithm.RS256.getValue().equals(alg)) {
                return rsaPublicKey;
            }
            if (SignatureAlgorithm.HS256.getValue().equals(alg) && secretKey != null && !secretKey.isBlank()) {
                return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
            }
            throw new UnsupportedJwtException("JWT algorithm not allowed: " + alg);
        }
    };
}
