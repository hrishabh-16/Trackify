package com.trackify.config;

import com.trackify.security.JwtAuthenticationEntryPoint;
import com.trackify.security.JwtAuthenticationFilter;
import com.trackify.security.OAuth2AuthenticationSuccessHandler;
import com.trackify.security.OAuth2UserService;
import com.trackify.security.CustomUserDetailsService;
import com.trackify.security.HttpCookieOAuth2AuthorizationRequestRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private JwtConfig jwtConfig;
    
    @Autowired
    private OAuth2UserService oAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF
            .csrf(AbstractHttpConfigurer::disable)
            
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Set session management to stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authentication provider
            .authenticationProvider(authenticationProvider())
            
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // Configure exception handling
            .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            
            // Configure OAuth2
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorize")
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository())
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/oauth2/callback/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )
            
         // Configure authorization with specific rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - NO authentication required
                .requestMatchers(
                    // Auth endpoints
                    "/auth/**",
                    "/api/auth/**",
                    
                    // OAuth2 endpoints
                    "/oauth2/**",
                    
                    // Test endpoints
                    "/test/**",
                    
                    // Debug endpoints (temporary - remove in production)
                    "/api/debug/**",
                    "/debug/**",
                    
                    // Actuator endpoints
                    "/actuator/**",
                    
                    // Swagger/OpenAPI endpoints - CORRECTED PATHS for /api context
                    "/api/swagger-ui/**",
                    "/api/swagger-ui.html",
                    "/api/swagger-ui/index.html",
                    "/api/v3/api-docs/**",
                    "/api/v3/api-docs.yaml",
                    "/api/v3/api-docs",
                    "/api/api-docs/**",
                    "/api/swagger-resources/**",
                    "/api/webjars/**",
                    
                    // Also allow direct access (for redirects)
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/v3/api-docs",
                    "/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    
                    // Static resources
                    "/favicon.ico",
                    "/error",
                    "/",
                    
                    // H2 Console (for development)
                    "/h2-console/**"
                ).permitAll()
                
                // User endpoints with specific rules - CORRECTED PATHS
                .requestMatchers("/users/me").authenticated() // Any authenticated user can access their own profile
                .requestMatchers("/users/**").hasRole("ADMIN") // Only admins can access user management
                
                // If you also have /api/users endpoints, add these too
                .requestMatchers("/api/users/me").authenticated()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                
                // Team endpoints - support both /api and direct paths
                .requestMatchers(HttpMethod.GET, "/teams/*/members/**").authenticated()
                .requestMatchers("/teams/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/teams/*/members/**").authenticated()
                .requestMatchers("/api/teams/**").authenticated()
                
                // Notification endpoints
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Disable unnecessary features
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }
}