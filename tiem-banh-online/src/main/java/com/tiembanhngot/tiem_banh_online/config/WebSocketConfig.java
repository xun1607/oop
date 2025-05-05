package com.tiembanhngot.tiem_banh_online.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Bật WebSocket message handling với message broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Bật một simple broker để gửi message đến client trên các destination bắt đầu bằng /topic
        config.enableSimpleBroker("/topic");
        // Định nghĩa tiền tố cho các message được gửi từ client đến server (ví dụ: client gửi đến /app/chat)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký endpoint "/ws" để client kết nối WebSocket đến
        // withSockJS() cung cấp fallback nếu trình duyệt không hỗ trợ WebSocket thuần
        registry.addEndpoint("/ws").withSockJS();
    }
}