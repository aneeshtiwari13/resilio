package com.resilio.platform.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 1. Static resources and health endpoints
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/api/health", "/api/version").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // 2. Operational mutations require OPERATOR or ADMIN role
                        .requestMatchers(HttpMethod.POST, "/api/recovery/**").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/releases/**").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/pipelines/**").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/incidents/**").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/demo/**").hasAnyRole("OPERATOR", "ADMIN")

                        // 3. Admin-only operations
                        .requestMatchers("/api/admin/**", "/api/users/**").hasRole("ADMIN")

                        // 4. Read operations permitted for VIEWER, OPERATOR, ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("VIEWER", "OPERATOR", "ADMIN")

                        // 5. Default: all other requests require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required to access this resource\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Insufficient permissions for user role\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID", "Idempotency-Key"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
