package com.dearlavion.storeengine.config;

import com.dearlavion.storeengine.security.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Route matcher layout mirrors the NestJS controllers' guard usage exactly (see the module
 * inventory in the plan): public GET/POST-recommendations/newsletter routes, authenticated-only
 * (surveys/cart/orders/profile/shipping-details/kits), and /admin/** requiring the ADMIN role.
 * {@link AuthenticationFilter} does the actual token verification and role assignment.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationFilter authenticationFilter;

    @Value("${app.frontend-origin}")
    private String frontendOrigin;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        // Spring Boot's default error handling does an internal FORWARD to /error
                        // (e.g. an unmapped 404, or our own AuthenticationFilter's earlier-considered
                        // sendError() path), which gets re-evaluated by this same filter chain. Left
                        // unpermitted, an anonymous request to any *public* route that 404s would come
                        // back as a misleading 403 instead of 404 once it hits /error unauthenticated.
                        .requestMatchers("/error").permitAll()
                        // Bare base path AND /** both listed: Spring's path matching treats
                        // "/products/**" as requiring at least a trailing segment — it does NOT
                        // match the bare "/products" with no trailing slash.
                        .requestMatchers(HttpMethod.GET,
                                "/products", "/products/**",
                                "/product-items", "/product-items/**",
                                "/categories", "/categories/**",
                                "/taxonomies", "/taxonomies/**",
                                "/popular-kits", "/popular-kits/**",
                                "/exchange-rates", "/exchange-rates/**",
                                "/store-settings", "/store-settings/**",
                                "/kit-settings",
                                "/stats/top-selling"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/survey/recommendations", "/newsletter/subscribe").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // surveys, cart, orders, profile, shipping-details, kits — AuthGuard only in NestJS.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(frontendOrigin, "https://*.ngrok.pizza"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", "Origin"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
