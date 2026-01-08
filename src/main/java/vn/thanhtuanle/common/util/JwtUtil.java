package vn.thanhtuanle.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.thanhtuanle.entity.User;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {

//    @Value("${application.security.jwt.secret-key}")
//    private String secretKey;
//
//    @Value("${application.security.jwt.expiration}")
//    private long jwtExpiration;
//
//    @Value("${application.security.jwt.refresh-token.expiration}")
//    private long refreshExpiration;
//
//    public String extractUsername(String jwtToken) {
//        return extractClaims(jwtToken, Claims::getSubject);
//    }
//
//    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
//        final Claims claims = extractAllClaims(token);
//        return claimsResolver.apply(claims);
//    }
//
//    public String generateToken(User userDetails) {
//        return generateToken(new HashMap<>(), userDetails);
//    }
//
//    public String generateToken(Map<String, Object> extractClaims, User userDetails) {
//        return buildToken(extractClaims, userDetails, jwtExpiration);
//    }
//
//    public String generateRefreshToken(User userDetails) {
//        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
//    }
//
//    private String buildToken(Map<String, Object> extractClaims, User user, long expiration) {
//        return Jwts
//                .builder()
//                .setClaims(extractClaims)
//                .setSubject(user.getUsername())
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + expiration))
//                .signWith(getSingInKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    public boolean isTokenValid(String token, UserDetails userDetails) {
//        try {
//            final String email = extractUsername(token);
//            return (email.equals(userDetails.getUsername())) && !isTokenExpired(token);
//        } catch (ExpiredJwtException e) {
//            return false;
//        }
//    }
//
//    public boolean isTokenExpired(String token) {
//        return extractExpiration(token).before(new Date());
//    }
//
//    private Date extractExpiration(String token) {
//        return extractClaims(token, Claims::getExpiration);
//    }
//
//    private Claims extractAllClaims(String token) {
//        return Jwts
//                .parserBuilder()
//                .setSigningKey(getSingInKey())
//                .setAllowedClockSkewSeconds(60)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    private Key getSingInKey() {
//        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
//        return Keys.hmacShaKeyFor(keyBytes);
//    }
}
