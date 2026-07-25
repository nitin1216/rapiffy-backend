package com.example.rapiffy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/* Owner -- Nitin Pandey */
@SpringBootApplication
@EnableScheduling
public class RapiffyApplication {

	public static void main(String[] args) {
		SpringApplication.run(RapiffyApplication.class, args);
		System.out.println("--------------------------------------------------");
		System.out.println("  Rapiffy Backend started successfully!");
		System.out.println("  Swagger UI: http://localhost:8080/swagger-ui.html");
		System.out.println("--------------------------------------------------");
	}
}
