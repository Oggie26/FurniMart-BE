package com.example.productservice.service;

import com.example.productservice.feign.AuthClient;
import com.example.productservice.response.AuthResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthClient authClient;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {


            AuthResponse user = authClient.getUserByUsername(email).getData();
            if (user == null || user.getEmail() == null || user.getPassword() == null) {
                throw new UsernameNotFoundException("User or password not found for: " + email);
            }

            return new CustomUserDetails(
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole()
            );

        } catch (FeignException.NotFound e) {
            log.warn("User not found in auth-service: {}", email);
            throw new UsernameNotFoundException("User not found: " + email);
        } catch (FeignException.Unauthorized e) {
            log.error("Unauthorized when fetching user from auth-service: {}", email);
            throw new UsernameNotFoundException("Unauthorized to fetch user: " + email);
        } catch (FeignException e) {
            log.error("Error calling auth-service: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user from auth-service", e);
        }
    }
}
