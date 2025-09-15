package api_gateway.api_gateway.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("user-service", r -> r.path("/api/auth/**")
                        .uri("lb://user-service"))

                .route("user-service", r -> r.path("/api/user/**")
                        .uri("lb://user-service"))

                .route("product-service", r -> r.path("/api/products/**")
                        .uri("lb://product-service"))

                .route("notification-service", r -> r.path("/api/notification/**")
                        .uri("lb://notification-service"))

                .route("order-service", r -> r.path("/api/order/**")
                        .uri("lb://order-service"))

                .route("payment-service", r -> r.path("/api/payment/**")
                        .uri("lb://payment-service"))

                .route("delivery-service", r -> r.path("/api/delivery/**")
                        .uri("lb://delivery-service"))

                .build();
    }
}
