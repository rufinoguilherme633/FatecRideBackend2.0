package com.example.fatecCarCarona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class FatecCarCaronaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FatecCarCaronaApplication.class, args);
	}

}
