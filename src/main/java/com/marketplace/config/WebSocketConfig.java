package com.marketplace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to carry messages back to the
        // client on destinations prefixed with /topic
        config.enableSimpleBroker("/topic", "/queue");

        // Designates the prefix for messages that are bound for methods annotated with
        // @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registers the "/ws" endpoint, enabling SockJS fallback options so that
        // alternate transports can be used if WebSocket is not available.
        // The SockJS client will attempt to connect to "/ws" and use the best available
        // transport (websocket, xhr-streaming, xhr-polling, etc)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins for now, restrict in production
                .withSockJS();
    }
}
