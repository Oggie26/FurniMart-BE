package api_gateway.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();

                    config.setAllowedOriginPatterns(List.of(
                            "http://localhost:5173",
                            "http://127.0.0.1:5173",
                            "http://localhost:3000",
                            "http://127.0.0.1:3000",
                            "http://152.53.169.79"
                    ));

                    config.setAllowedMethods(List.of(
                            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
                    ));

                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("Authorization", "Content-Type"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);

                    return config;
                }))

                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())

                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/api/products/**",
                                "/api/auth/**"
                        ).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyExchange().authenticated()
                );

        return http.build();
    }
}
