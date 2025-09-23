    package api_gateway.api_gateway.config;

    import lombok.RequiredArgsConstructor;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
    import org.springframework.web.cors.CorsConfiguration;
    import org.springframework.web.cors.reactive.CorsWebFilter;
    import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

    import java.util.List;

    @Configuration
    @EnableReactiveMethodSecurity
    @RequiredArgsConstructor
    public class SecurityConfig {

//        private final JwtAuthenticationFilter jwtFilter;
//
//        @Bean
//        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//            http
//                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//                    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//                    .authorizeExchange(exchanges -> exchanges
//                            .pathMatchers(
//                                    "/swagger-ui.html",
//                                    "/swagger-ui/**",
//                                    "/v3/api-docs/**",
//                                    "/webjars/**",
//                                    "/api/auth/**"
//                            ).permitAll()
//                            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//                            .anyExchange().authenticated()
//                    )
//                    .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);
//
//            return http.build();
//        }

        @Bean
        public CorsWebFilter corsWebFilter() {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of(
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://localhost:3000",
                    "http://127.0.0.1:3000",
                    "http://152.53.169.79",
                    "http://152.53.169.79:8080"
            ));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            config.addAllowedHeader("*");
            config.setExposedHeaders(List.of("Authorization", "Content-Type"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", config);

            return new CorsWebFilter(source);
        }
    }
