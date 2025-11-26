package com.example.userservice.config;

import com.example.userservice.websocket.ChatWebSocketHandler;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer, ServletContextAware {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private ServletContext servletContext;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*") // Use patterns instead of origins to allow credentials
                .withSockJS(); // Enable SockJS fallback
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        // Check if ServletContext is available
        if (servletContext == null) {
            log.warn("ServletContext is not available. WebSocket container will NOT be created. This may occur in test environments.");
            return null;
        }
        
        if (servletContext.getAttribute("jakarta.websocket.server.ServerContainer") == null) {
            log.warn("WebSocket ServerContainer attribute not found in ServletContext. WebSocket container will NOT be created. " +
                    "This may occur if WebSocket support is not available in the servlet container.");
            return null;
        }
        
        log.info("Creating WebSocket container with maxTextMessageBufferSize=8192, maxBinaryMessageBufferSize=8192, maxSessionIdleTimeout=300000ms");
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(300000L); // 5 minutes
        return container;
    }
}
