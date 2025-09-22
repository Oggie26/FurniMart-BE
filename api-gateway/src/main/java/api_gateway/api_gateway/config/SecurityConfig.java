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
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .cors(ServerHttpSecurity.CorsSpec::disable)
                    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                    .authorizeExchange(exchanges -> exchanges
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
