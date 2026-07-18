package com.ban.vehicle_management.infrastructure.realtime.chat;

import com.ban.vehicle_management.infrastructure.security.jwt.JwtAuthenticationConverter;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public ChatWebSocketConfig(
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();
                if (StompCommand.CONNECT.equals(command)) {
                    accessor.setUser(authenticate(accessor));
                } else if ((StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command))
                        && accessor.getUser() == null) {
                    throw new AccessDeniedException("Access is denied");
                }

                return message;
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String token = resolveBearerToken(accessor)
                .orElseThrow(() -> new AccessDeniedException("Missing websocket access token"));
        AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwtDecoder.decode(token));
        String principalName = resolveAccountId(authentication)
                .orElseThrow(() -> new AccessDeniedException("Websocket account_id is required"));
        UsernamePasswordAuthenticationToken websocketAuthentication = new UsernamePasswordAuthenticationToken(
                principalName,
                authentication.getCredentials(),
                authentication.getAuthorities()
        );
        websocketAuthentication.setDetails(authentication.getDetails());
        return websocketAuthentication;
    }

    private Optional<String> resolveBearerToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            authorization = accessor.getFirstNativeHeader("authorization");
        }
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(authorization.substring("Bearer ".length()).trim())
                .filter(StringUtils::hasText);
    }

    private Optional<String> resolveAccountId(AbstractAuthenticationToken authentication) {
        if (authentication.getDetails() instanceof AuthenticatedAccountPrincipal principal
                && principal.accountId() != null) {
            return Optional.of(principal.accountId().toString());
        }
        return Optional.empty();
    }
}
