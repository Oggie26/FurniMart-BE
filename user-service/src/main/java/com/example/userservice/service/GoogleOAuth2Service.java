package com.example.userservice.service;

import com.example.userservice.config.JwtService;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.entity.UserStore;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.UserStoreRepository;
import com.example.userservice.response.LoginResponse;
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
    private final UserStoreRepository userStoreRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

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

            // Generate JWT tokens
            List<UserStore> userStores = userStoreRepository.findByUserId(account.getUser().getId());
            List<String> storeIds = userStores.stream()
                    .map(us -> us.getStore().getId())
                    .toList();

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

        userRepository.save(user);

        log.info("Created new user from Google OAuth: {}", userInfo.getEmail());
        return savedAccount;
    }

    @lombok.Builder
    @lombok.Data
    private static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;
        private String googleId;
    }
}

