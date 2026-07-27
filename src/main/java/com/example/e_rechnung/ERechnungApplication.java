package com.example.e_rechnung;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example")
public class ERechnungApplication {

	public static void main(String[] args) {
		SpringApplication.run(ERechnungApplication.class, args);
	}

}
