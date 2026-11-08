package com.chatapp.chat_backend.security.jwt;

import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.security.authentication.CustomReactiveUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtService jwtService;
    private final CustomReactiveUserDetailsService userDetailsService;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            @NonNull WebFilterChain chain
    ) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            return unauthorized(exchange);
        }

        UUID userId;
        try {
            userId = jwtService.extractUserId(token);
        } catch (Exception exception) {
            return unauthorized(exchange);
        }

        return userDetailsService.findByUserId(userId)
                .flatMap(userDetails -> {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    return chain.filter(exchange)
                            .contextWrite(
                                    ReactiveSecurityContextHolder.withAuthentication(authentication)
                            );
                })
                .onErrorResume(
                        BusinessException.class,
                        exception -> unauthorized(exchange)
                );
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
        return exchange.getResponse().setComplete();
    }
}
