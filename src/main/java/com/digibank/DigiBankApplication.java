package com.digibank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DigiBankApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigiBankApplication.class, args);
	}
}
