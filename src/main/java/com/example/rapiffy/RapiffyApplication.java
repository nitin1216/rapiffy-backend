package com.example.rapiffy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/* Owner -- Nitin Pandey */

@SpringBootApplication
@EnableScheduling
public class RapiffyApplication {

	private static final Logger log = LoggerFactory.getLogger(RapiffyApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(RapiffyApplication.class, args);
		log.info("--------------------------------------------------");
		log.info("  Rapiffy Backend started successfully!");
		log.info("  Swagger UI: http://localhost:8080/swagger-ui.html");
		log.info("--------------------------------------------------");
	}
}
