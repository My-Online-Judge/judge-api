package vn.thanhtuanle.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.transaction.Transactional;
import vn.thanhtuanle.auth.dto.AuthResponse;
import vn.thanhtuanle.auth.dto.ExchangeTokenResponse;
import vn.thanhtuanle.auth.dto.FullUserInfo;
import vn.thanhtuanle.auth.dto.IntrospectRequest;
import vn.thanhtuanle.auth.dto.IntrospectResponse;
import vn.thanhtuanle.common.enums.CommonStatus;
import vn.thanhtuanle.common.enums.Role;
import vn.thanhtuanle.common.enums.TokenType;
import vn.thanhtuanle.common.exception.AppException;
import vn.thanhtuanle.common.exception.ErrorCode;
import vn.thanhtuanle.common.util.JwtUtil;
import vn.thanhtuanle.entity.Token;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.repository.TokenRepository;
import vn.thanhtuanle.user.RoleRepository;
import vn.thanhtuanle.user.UserRepository;
import vn.thanhtuanle.user.dto.UserResponse;
import vn.thanhtuanle.user.mapper.UserMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final TokenRepository tokenRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.google.authorization-uri}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    public String getGoogleAuthUrl() {
        return authorizationUri + "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code&scope=email%20profile%20openid&access_type=offline";
    }

    @Transactional
    public AuthResponse authenticateGoogleUser(String code) {
        // 1. Exchange code for tokens
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ExchangeTokenResponse tokenResponse;
        try {
            ResponseEntity<ExchangeTokenResponse> response = restTemplate.postForEntity(tokenUri, request,
                    ExchangeTokenResponse.class);
            tokenResponse = response.getBody();
        } catch (Exception e) {
            log.error("Failed to exchange token with Google", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // 2. Get User Info
        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(tokenResponse.getAccessToken());
        HttpEntity<String> userInfoRequest = new HttpEntity<>(userInfoHeaders);

        FullUserInfo googleUser;
        try {
            ResponseEntity<FullUserInfo> userInfoResponse = restTemplate.exchange(userInfoUri, HttpMethod.GET,
                    userInfoRequest, FullUserInfo.class);
            googleUser = userInfoResponse.getBody();
        } catch (Exception e) {
            log.error("Failed to get user info from Google", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        if (googleUser == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // 3. Find or Create User
        User user = userRepository.findByEmail(googleUser.getEmail()).orElseGet(() -> {
            var role = roleRepository.findByName(Role.USER.getValue())
                    .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

            return userRepository.save(User.builder()
                    .name(googleUser.getName())
                    .username(googleUser.getEmail())
                    .email(googleUser.getEmail())
                    .avatar(googleUser.getPicture())
                    .googleId(googleUser.getSub())
                    .status(CommonStatus.ACTIVE.getValue())
                    .roles(new HashSet<>(Collections.singletonList(role)))
                    .build());
        });

        // Update info if needed
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleUser.getSub());
            if (user.getAvatar() == null)
                user.setAvatar(googleUser.getPicture());
            userRepository.save(user);
        }

        // 4. Generate Tokens
        var jwtToken = jwtUtil.generateToken(user);
        var refreshToken = jwtUtil.generateRefreshToken(user);

        revokeAllUserTokens(user);
        savedUserToken(user, jwtToken, TokenType.ACCESS);
        savedUserToken(user, refreshToken, TokenType.REFRESH);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void savedUserToken(User user, String jwtToken, TokenType tokenType) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(tokenType)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        tokenRepository.deleteAllByUserId(user.getId());
    }

    public UserResponse me() {
        log.info("Fetching current user information");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new AppException(ErrorCode.USER_NOT_EXISTED);
                });

        log.info("User information retrieved for username: {}", username);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void logout() {
        log.info("Logout request received");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new AppException(ErrorCode.USER_NOT_EXISTED);
                });

        revokeAllUserTokens(user);
        log.info("User logged out successfully");
    }

    public IntrospectResponse introspect(IntrospectRequest request) {
        var token = request.getToken();
        boolean isValid = true;

        try {
            if (jwtUtil.isTokenExpired(token))
                isValid = false;
        } catch (Exception e) {
            isValid = false;
        }

        if (isValid) {
            var storedToken = tokenRepository.findByToken(token)
                    .orElse(null);
            if (storedToken == null || storedToken.isRevoked() || storedToken.isExpired()
                    || !storedToken.getTokenType().equals(request.getTokenType())) {
                isValid = false;
            }
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }
}
