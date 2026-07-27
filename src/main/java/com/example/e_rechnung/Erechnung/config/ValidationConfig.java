package com.example.e_rechnung.Erechnung.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;

@Configuration
public class ValidationConfig {
    @Bean
    public SchemaFactory schemaFactory() {
        return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    }
}