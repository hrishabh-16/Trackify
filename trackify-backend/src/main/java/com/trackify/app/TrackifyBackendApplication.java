package com.trackify.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(scanBasePackages = "com.trackify")
@EntityScan(basePackages = "com.trackify.entity")
@EnableJpaRepositories(basePackages = "com.trackify.repository")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
@Slf4j
public class TrackifyBackendApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(TrackifyBackendApplication.class);
    
    public static void main(String[] args) {
        SpringApplication.run(TrackifyBackendApplication.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void logStartup() {
        logger.info("=========================================================");
        logger.info("Trackify Backend Application started successfully!");
        logger.info("Server running on: http://localhost:8090");
        logger.info("API Base URL: http://localhost:8090/api");
        logger.info("Swagger UI: http://localhost:8090/api/swagger-ui.html");
        logger.info("API Docs: http://localhost:8090/api/api-docs");
        logger.info("=========================================================");
    }
}