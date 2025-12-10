package com.example.userservice.service;

import com.example.userservice.config.JwtService;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.inteface.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final WalletService walletService;

    @Transactional
    public LoginResponse authenticateWithGoogle(String googleAccessToken) {
        try {
            // Verify the token with Google and get user info
            GoogleUserInfo userInfo = getGoogleUserInfo(googleAccessToken);
            
            if (userInfo == null || userInfo.getEmail() == null) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            // Check if user exists
            Account account = accountRepository.findByEmailAndIsDeletedFalse(userInfo.getEmail())
                    .orElse(null);

            // If user doesn't exist, create a new account
            if (account == null) {
                account = createGoogleUser(userInfo);
            } else {
                // Check if account is blocked or deleted
                if (EnumStatus.INACTIVE.equals(account.getStatus())) {
                    throw new AppException(ErrorCode.USER_BLOCKED);
                }
                if (EnumStatus.DELETED.equals(account.getStatus())) {
                    throw new AppException(ErrorCode.USER_DELETED);
                }
            }

            List<String> storeIds = List.of();

            Map<String, Object> claims = Map.of(
                    "role", account.getRole(),
                    "userId", account.getId(),
                    "storeId", storeIds
            );
            
            String accessToken = jwtService.generateToken(claims, account.getEmail());
            String refreshToken = jwtService.generateRefreshToken(claims, account.getEmail());

            tokenService.saveToken(account.getEmail(), accessToken, jwtService.getJwtExpiration());
            tokenService.saveRefreshToken(account.getEmail(), refreshToken, jwtService.getRefreshExpiration());

            return LoginResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (Exception e) {
            log.error("Error during Google OAuth authentication: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    private GoogleUserInfo getGoogleUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return GoogleUserInfo.builder()
                        .email(jsonNode.get("email").asText())
                        .name(jsonNode.has("name") ? jsonNode.get("name").asText() : null)
                        .picture(jsonNode.has("picture") ? jsonNode.get("picture").asText() : null)
                        .googleId(jsonNode.has("id") ? jsonNode.get("id").asText() : null)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error fetching Google user info: {}", e.getMessage());
        }
        return null;
    }

    private Account createGoogleUser(GoogleUserInfo userInfo) {
        // Create account
        Account account = Account.builder()
                .email(userInfo.getEmail())
                .password("GOOGLE_OAUTH") // Placeholder, not used for Google OAuth
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        Account savedAccount = accountRepository.save(account);

        // Create user
        User user = User.builder()
                .fullName(userInfo.getName() != null ? userInfo.getName() : userInfo.getEmail())
                .phone(null) // Google doesn't provide phone by default
                .status(EnumStatus.ACTIVE)
                .avatar(userInfo.getPicture())
                .point(0)
                .account(savedAccount)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-create wallet for new customer
        try {
            walletService.createWalletForUser(savedUser.getId());
            log.info("Wallet auto-created for new Google OAuth user: {}", savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to auto-create wallet for Google OAuth user {}: {}", savedUser.getId(), e.getMessage());
            // Don't fail user creation if wallet creation fails, but log the error
        }

        log.info("Created new user from Google OAuth: {}", userInfo.getEmail());
        return savedAccount;
    }

    private static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;
        private String googleId;

        // Manual getters (Lombok not working in Docker build)
        @SuppressWarnings("unused")
        public String getEmail() { return email; }
        @SuppressWarnings("unused")
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        @SuppressWarnings("unused")
        public void setName(String name) { this.name = name; }
        public String getPicture() { return picture; }
        @SuppressWarnings("unused")
        public void setPicture(String picture) { this.picture = picture; }
        @SuppressWarnings("unused")
        public String getGoogleId() { return googleId; }
        @SuppressWarnings("unused")
        public void setGoogleId(String googleId) { this.googleId = googleId; }

        // Builder pattern
        public static GoogleUserInfoBuilder builder() {
            return new GoogleUserInfoBuilder();
        }

        public static class GoogleUserInfoBuilder {
            private String email;
            private String name;
            private String picture;
            private String googleId;

            public GoogleUserInfoBuilder email(String email) { this.email = email; return this; }
            public GoogleUserInfoBuilder name(String name) { this.name = name; return this; }
            public GoogleUserInfoBuilder picture(String picture) { this.picture = picture; return this; }
            public GoogleUserInfoBuilder googleId(String googleId) { this.googleId = googleId; return this; }

            public GoogleUserInfo build() {
                GoogleUserInfo info = new GoogleUserInfo();
                info.email = this.email;
                info.name = this.name;
                info.picture = this.picture;
                info.googleId = this.googleId;
                return info;
            }
        }
    }
}

