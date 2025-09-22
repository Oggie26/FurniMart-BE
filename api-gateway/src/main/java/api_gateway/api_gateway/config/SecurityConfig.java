package api_gateway.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // Disable CSRF
                .csrf(csrf -> csrf.disable())

                // CORS configuration
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(java.util.List.of(
                            "http://localhost:5173",
                            "http://127.0.0.1:5173",
                            "http://152.53.169.79"
                    ));
                    corsConfig.setAllowedMethods(java.util.List.of(
                            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
                    ));
                    corsConfig.setAllowedHeaders(java.util.List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setMaxAge(3600L);
                    return corsConfig;
                }))

                // Disable default login & basic auth
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())

                // Authorize requests
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/api/auth/**"
                        ).permitAll()
                        // OPTIONS requests must be permitted for CORS preflight
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // All other requests require authentication
                        .anyExchange().authenticated()
                );

        return http.build();
    }
}
