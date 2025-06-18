package com.trackify.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        logger.debug("Processing request: {} {}", method, requestURI);
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                logger.debug("JWT token found, validating...");
                
                if (jwtTokenProvider.validateToken(jwt)) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                    logger.debug("Valid JWT token for user ID: {}", userId);
                    
                    // Load user details which includes authorities
                    UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                    logger.debug("Loaded user: {} with authorities: {}", 
                        userDetails.getUsername(), userDetails.getAuthorities());
                    
                    // Create authentication with authorities from UserDetails
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.debug("Successfully set authentication for user: {} on request: {} {}", 
                        userDetails.getUsername(), method, requestURI);
                } else {
                    logger.warn("Invalid JWT token for request: {} {}", method, requestURI);
                }
            } else {
                logger.debug("No JWT token found for request: {} {}", method, requestURI);
            }
        } catch (Exception ex) {
            logger.error("Error setting authentication for request: {} {}", method, requestURI, ex);
            // Clear the security context on error
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // Log the exact header value for debugging
        if (bearerToken != null) {
            logger.debug("Authorization header present: 'Bearer ***' (length: {})", bearerToken.length());
            
            // Check for exact "Bearer " prefix (with space)
            if (bearerToken.startsWith("Bearer ") && bearerToken.length() > 7) {
                String token = bearerToken.substring(7).trim(); // Also trim any extra spaces
                logger.debug("Extracted JWT token (length: {})", token.length());
                return token;
            } else {
                logger.warn("Authorization header format invalid. Expected 'Bearer <token>', got: '{}'", 
                    bearerToken.substring(0, Math.min(20, bearerToken.length())) + "...");
            }
        } else {
            logger.debug("No Authorization header found");
        }
        
        return null;
    }
}