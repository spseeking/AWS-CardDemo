package com.carddemo.batch.online.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Turns the {@code Authorization: Bearer} token handed out by sign on into an authentication. */
@Component
public class SignOnTokenFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final SignOnSessions sessions;

    public SignOnTokenFilter(SignOnSessions sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            sessions.authenticate(header.substring(PREFIX.length()).trim())
                    .ifPresent(authentication -> SecurityContextHolder.getContext()
                            .setAuthentication(authentication));
        }
        chain.doFilter(request, response);
    }
}
