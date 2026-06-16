package com.qrrestaurant.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final UUID USER_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    private final JwtService jwtService = mock(JwtService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWhenJwtCookieIsValid() throws Exception {
        String token = "valid.jwt.token";
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(USER_ID);
        HttpServletRequest request = requestWithCookies(new Cookie("jwt", token));
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(USER_ID);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenNoCookieIsPresent() throws Exception {
        HttpServletRequest request = requestWithCookies((Cookie[]) null);

        filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotAuthenticateWhenJwtCookieIsInvalid() throws Exception {
        String token = "tampered.jwt.token";
        when(jwtService.isTokenValid(token)).thenReturn(false);
        HttpServletRequest request = requestWithCookies(new Cookie("jwt", token));

        filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUserId(token);
    }

    @Test
    void shouldIgnoreUnrelatedCookies() throws Exception {
        HttpServletRequest request = requestWithCookies(new Cookie("session", "unrelated"));

        filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).isTokenValid("unrelated");
    }

    private HttpServletRequest requestWithCookies(Cookie cookie) {
        return requestWithCookies(cookie == null ? null : new Cookie[]{cookie});
    }

    private HttpServletRequest requestWithCookies(Cookie[] cookies) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(cookies);
        return request;
    }
}
