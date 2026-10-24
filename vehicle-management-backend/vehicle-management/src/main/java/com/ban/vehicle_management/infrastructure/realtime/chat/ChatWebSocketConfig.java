package com.ban.vehicle_management.infrastructure.realtime.chat;

import com.ban.vehicle_management.infrastructure.security.jwt.JwtAuthenticationConverter;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.realtime.security.WebSocketActorPrincipal;
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
    private final AccountAuthorizationPortOut accountAuthorizationPortOut;

    public ChatWebSocketConfig(
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AccountAuthorizationPortOut accountAuthorizationPortOut
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.accountAuthorizationPortOut = accountAuthorizationPortOut;
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
                } else if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
                    revalidate(accessor);
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

    Authentication authenticate(StompHeaderAccessor accessor) {
        String token = resolveBearerToken(accessor)
                .orElseThrow(() -> new AccessDeniedException("Missing websocket access token"));
        AbstractAuthenticationToken authentication;
        try {
            authentication = jwtAuthenticationConverter.convert(jwtDecoder.decode(token));
        } catch (RuntimeException exception) {
            throw new AccessDeniedException("Invalid websocket access token", exception);
        }
        AuthenticatedAccountPrincipal authenticatedPrincipal = resolvePrincipal(authentication)
                .orElseThrow(() -> new AccessDeniedException("Websocket account access is required"));
        CurrentAccountAccess snapshot = Optional.ofNullable(authenticatedPrincipal.accessSnapshot())
                .orElseThrow(() -> new AccessDeniedException("Websocket account access is required"));
        WebSocketActorPrincipal actor = new WebSocketActorPrincipal(
                snapshot.accountId(), snapshot, snapshot.accessRevision());
        UsernamePasswordAuthenticationToken websocketAuthentication = new UsernamePasswordAuthenticationToken(
                actor,
                authentication.getCredentials(),
                authentication.getAuthorities()
        );
        websocketAuthentication.setDetails(actor);
        return websocketAuthentication;
    }

    void revalidate(StompHeaderAccessor accessor) {
        WebSocketActorPrincipal actor = resolveActor(accessor.getUser())
                .orElseThrow(() -> new AccessDeniedException("Access is denied"));
        CurrentAccountAccess current = accountAuthorizationPortOut.findByAccountId(actor.accountId())
                .orElseThrow(() -> new AccessDeniedException("Access is denied"));
        if (!actor.accessRevision().equals(current.accessRevision())) {
            throw new AccessDeniedException("Websocket access changed; reconnect is required");
        }
    }

    private Optional<WebSocketActorPrincipal> resolveActor(java.security.Principal user) {
        if (user instanceof Authentication authentication) {
            if (authentication.getDetails() instanceof WebSocketActorPrincipal actor) {
                return Optional.of(actor);
            }
            if (authentication.getPrincipal() instanceof WebSocketActorPrincipal actor) {
                return Optional.of(actor);
            }
        }
        return user instanceof WebSocketActorPrincipal actor ? Optional.of(actor) : Optional.empty();
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

    private Optional<AuthenticatedAccountPrincipal> resolvePrincipal(AbstractAuthenticationToken authentication) {
        if (authentication.getDetails() instanceof AuthenticatedAccountPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }
}
