package com.example.e_rechnung.Erechnung.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @PostConstruct
    public void log() {
        System.out.println("✅✅✅ WebConfig wurde geladen! ✅✅✅");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        System.out.println("🔥 CORS wird mit allowedOriginPatterns konfiguriert!");
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:3001",
                        "https://e-rechnung-frontend.onrender.com"  // 🔥 Frontend-URL
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}