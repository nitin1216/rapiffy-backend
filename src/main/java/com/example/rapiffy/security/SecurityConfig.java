package com.example.rapiffy.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless JWT APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless — no sessions, every request must carry a JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Define which routes are open vs protected
            .authorizeHttpRequests(auth -> auth
                // Open: signup and login (including Google)
                .requestMatchers("/v1/auth/**").permitAll()
                // Open: customer browsing (no login needed)
                .requestMatchers("/v1/customer/shops/**").permitAll()
                .requestMatchers("/v1/customer/categories").permitAll()
                .requestMatchers("/v1/customer/catalog/**").permitAll()
                .requestMatchers("/v1/customer/products", "/v1/customer/products/**").permitAll()
                // Open: Razorpay webhook (called by Razorpay servers, not users)
                .requestMatchers("/v1/webhook/**").permitAll()
                // Auth required: customer orders and cart
                .requestMatchers("/v1/customer/orders/**").authenticated()
                .requestMatchers("/v1/customer/cart/**").authenticated()
                .requestMatchers("/v1/customer/wishlist/**").authenticated()
                .requestMatchers("/v1/customer/profile/**").authenticated()
                .requestMatchers("/v1/customer/payment/**").authenticated()
                // Open: Swagger UI
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // Open: static files (HTML, CSS, JS in /static folder)
                .requestMatchers("/**.html", "/**.css", "/**.js").permitAll()
                // SuperAdmin only
                .requestMatchers("/v1/super-admin/**").hasRole("SUPER_ADMIN")
                // Platform commission management (Platform role only)
                .requestMatchers("/v1/platform/**").hasRole("PLATFORM")
                // Admin (shopkeeper) only
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Add JWT filter before Spring's default auth filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
