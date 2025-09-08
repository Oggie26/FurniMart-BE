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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            AuthResponse user = authClient.getUserByUsername(username).getData();
            if (user.getPassword() == null) {
                throw new UsernameNotFoundException("User or password not found");
            }

            return new CustomUserDetails(
                    user.getUsername(),
                    user.getPassword(),
                    user.getRole()
            );

        } catch (FeignException.NotFound e) {
            throw new UsernameNotFoundException("User not found: " + username);
        } catch (FeignException e) {
            throw new RuntimeException("Failed to fetch user from auth-service", e);
        }
    }
}
