package com.example.e_rechnung.Erechnung.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Webhook ohne Authentifizierung erlauben (da der Aufruf direkt von Clerk erfolgt)
                        .requestMatchers("/api/webhooks/clerk").permitAll()
                        // Alle anderen Endpunkte erfordern eine Authentifizierung
                        .anyRequest().authenticated()
                )
                // Validierung von JWT-Tokens ausstellen durch Clerk aktivieren
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        // CSRF-Schutz deaktivieren, da JWT verwendet wird (und keine Sessions/Cookies zum Einsatz kommen)
        http.csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}