package com.trackify.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Value("${server.port:8090}")
    private String serverPort;
    
    @Value("${server.servlet.context-path:/api}")
    private String contextPath;
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(List.of(
                    new Server()
                        .url("http://localhost:" + serverPort + contextPath)
                        .description("Local Development Server"),
                    new Server()
                        .url("https://api.trackify.com")
                        .description("Production Server")
                ))
                .info(new Info()
                        .title("Trackify API")
                        .description("Real-time Expense Tracker REST API Documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Trackify Team")
                                .email("support@trackify.com")
                                .url("https://github.com/hrishabh-16/Trackify"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"")));
    }
}