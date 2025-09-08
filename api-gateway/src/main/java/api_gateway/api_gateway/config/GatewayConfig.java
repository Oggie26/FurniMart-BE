package api_gateway.api_gateway.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

import java.util.Arrays;
import java.util.List;


@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri("lb://auth-service"))

                .route("user-service", r -> r.path("/api/user/**")
                        .filters(f -> f.filter( jwtAuthenticationFilter))
                        .uri("lb://user-service"))

                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f.filter( jwtAuthenticationFilter))
                        .uri("lb://product-service"))

                .route("notification-service", r -> r.path("/api/notification/**")
                        .filters(f -> f.filter( jwtAuthenticationFilter))
                        .uri("lb://product-service"))

                .build();
    }
}
