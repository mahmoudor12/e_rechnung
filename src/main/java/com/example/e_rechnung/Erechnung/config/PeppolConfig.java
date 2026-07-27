package com.example.e_rechnung.Erechnung.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "peppol")
@Data
public class PeppolConfig {
    private String accessPointUrl;
    private String participantId;
    private String clientCertificatePath;
    private String clientPrivateKeyPath;
}