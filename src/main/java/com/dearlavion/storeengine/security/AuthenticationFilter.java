package com.dearlavion.storeengine.security;

import com.dearlavion.storeengine.common.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Populates the SecurityContext from a Bearer token, delegating verification to auth-service.
 * Ported from NestJS's resolveUser()/AuthGuard/OptionalAuthGuard/AdminGuard (src/auth/auth.ts):
 *
 * <ul>
 *   <li>No token at all → let the request through unauthenticated. Whether that's then rejected
 *       depends on the route's matcher in {@link com.dearlavion.storeengine.config.SecurityConfig}
 *       (.authenticated() → Spring's default entry point 403s it; .permitAll() → allowed, the
 *       OptionalAuthGuard case, e.g. the public survey).</li>
 *   <li>Token present but invalid/unverifiable, or valid but wrong tenant → 401 immediately, even
 *       on an otherwise-public route. This matches resolveUser() throwing UnauthorizedException
 *       unconditionally on a bad token, regardless of which guard called it.</li>
 *   <li>Valid token → SecurityContext gets an {@link AuthenticatedUser} principal, always with
 *       ROLE_USER, plus ROLE_ADMIN when role ∈ {ADMIN,STAFF} OR username is in the
 *       ADMIN_USERNAMES allowlist (AdminGuard's OR logic — the allowlist is a bootstrap escape
 *       hatch, not the primary signal).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "STAFF");

    private final AuthClientService authClient;
    private final ObjectMapper objectMapper;

    @Value("${app.admin-usernames:admin}")
    private String adminUsernamesRaw;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        VerifyResponse verification = authClient.verify(token);
        if (verification == null || !verification.isValid() || verification.getUserId() == null) {
            writeUnauthorized(response, "Invalid token");
            return;
        }

        String expectedCustomer = authClient.expectedCustomer();
        if (expectedCustomer != null && !expectedCustomer.isBlank()
                && !expectedCustomer.equals(verification.getCustomer())) {
            writeUnauthorized(response, "Token does not belong to this tenant");
            return;
        }

        AuthenticatedUser user = new AuthenticatedUser(
                verification.getUserId(),
                verification.getUsername() == null ? "" : verification.getUsername(),
                verification.getEmail(),
                verification.getActiveProfile(),
                verification.getCustomer()
        );

        boolean isAdmin = (user.role() != null && ADMIN_ROLES.contains(user.role()))
                || adminUsernames().contains(user.username());

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private Set<String> adminUsernames() {
        return Arrays.stream(adminUsernamesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Writes the 401 response directly rather than calling response.sendError(). sendError()
     * triggers Spring Boot's internal forward to /error, which gets re-evaluated by this same
     * security filter chain (as a FORWARD dispatch) — since no Authentication was ever set, that
     * second pass hits the default entry point and silently clobbers the 401 with a 403. Writing
     * the response body ourselves sidesteps that redispatch entirely, and also lets the error body
     * match GlobalExceptionHandler's ApiError shape instead of Spring Boot's default /error shape.
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        ApiError body = new ApiError(HttpStatus.UNAUTHORIZED.value(), message, HttpStatus.UNAUTHORIZED.getReasonPhrase());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
