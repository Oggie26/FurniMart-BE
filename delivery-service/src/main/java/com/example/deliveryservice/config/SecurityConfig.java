package com.example.deliveryservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    // Use AntPathRequestMatcher explicitly to avoid MvcRequestMatcher
                    // Pass null as HttpMethod to match all HTTP methods
                    RequestMatcher[] publicMatchers = {
                            new AntPathRequestMatcher("/api/auth/**", null),
                            new AntPathRequestMatcher("/swagger-ui/**", null),
                            new AntPathRequestMatcher("/v3/api-docs/**", null),
                            new AntPathRequestMatcher("/api/users/info/*", null),
                            new AntPathRequestMatcher("/swagger-ui.html", null),
                            new AntPathRequestMatcher("/static/**", null),
                            new AntPathRequestMatcher("/*.js", null),
                            new AntPathRequestMatcher("/*.css", null),
                            // Use RegexRequestMatcher for branch-info to completely avoid MvcRequestMatcher
                            new RegexRequestMatcher("/api/delivery/stores/[^/]+/branch-info", null)
                    };
                    auth.requestMatchers(publicMatchers).permitAll();
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
