package com.trackify.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrackifyBackendApplication {

	private static final Logger logger = LoggerFactory.getLogger(TrackifyBackendApplication.class);
		
	public static void main(String[] args) {
		SpringApplication.run(TrackifyBackendApplication.class, args);
	}
	
	  @EventListener(ApplicationReadyEvent.class)
	    public void logStartup() {
	        logger.info("Trackify App Backend started successfully on port 6000");
	        logger.info("Application '{}' is running!", "trackify-backend");
	    }

}
