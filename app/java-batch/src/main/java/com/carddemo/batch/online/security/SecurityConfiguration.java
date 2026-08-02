package com.carddemo.batch.online.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * CICS gave every transaction its security check before the program ran; the converted endpoints
 * need the same gate. Only sign on is anonymous, the COUSR* user administration screens are
 * restricted to SEC-USR-TYPE {@code A}, and everything else needs a signed on identity.
 */
@Configuration
public class SecurityConfiguration {

    /**
     * A filter bean is otherwise also registered for every URL, so a bearer token on a request that
     * no security chain matches would set an authentication nothing ever clears off the thread.
     */
    @Bean
    public FilterRegistrationBean<SignOnTokenFilter> signOnTokenFilterRegistration(SignOnTokenFilter filter) {
        FilterRegistrationBean<SignOnTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http, SignOnTokenFilter tokenFilter) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/signon").permitAll()
                        .requestMatchers("/api/users/**", "/api/users").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
