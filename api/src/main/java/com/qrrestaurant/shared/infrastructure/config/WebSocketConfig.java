package com.qrrestaurant.shared.infrastructure.config;
import com.qrrestaurant.shared.infrastructure.web.AllowedOriginResolver;
import com.qrrestaurant.shared.infrastructure.ws.WsAuthHandshakeInterceptor;
import com.qrrestaurant.shared.infrastructure.ws.WsSubscriptionSecurityInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AllowedOriginResolver allowedOriginResolver;
    private final WsAuthHandshakeInterceptor wsAuthHandshakeInterceptor;
    private final WsSubscriptionSecurityInterceptor wsSubscriptionSecurityInterceptor;

    public WebSocketConfig(AllowedOriginResolver allowedOriginResolver,
                           WsAuthHandshakeInterceptor wsAuthHandshakeInterceptor,
                           WsSubscriptionSecurityInterceptor wsSubscriptionSecurityInterceptor) {
        this.allowedOriginResolver = allowedOriginResolver;
        this.wsAuthHandshakeInterceptor = wsAuthHandshakeInterceptor;
        this.wsSubscriptionSecurityInterceptor = wsSubscriptionSecurityInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Chaque SUBSCRIBE est validé : ownership pour /topic/restaurants/**,
        // capacité (orderId non devinable) pour /topic/orders/{id}, refus sinon.
        // Un refus (exception dans preSend) émet une frame STOMP ERROR.
        registration.interceptors(wsSubscriptionSecurityInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOriginResolver.resolve().toArray(String[]::new))
                .addInterceptors(wsAuthHandshakeInterceptor)
                .withSockJS();
    }
}
