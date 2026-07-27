package com.example.e_rechnung.Erechnung.client.peppol;

import com.example.e_rechnung.Erechnung.config.PeppolConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PeppolAccessPointClient {

    private final PeppolConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

    public String submitInvoice(byte[] xml, String receiverId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<byte[]> entity = new HttpEntity<>(xml, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                config.getAccessPointUrl() + "/submit?receiver=" + receiverId,
                HttpMethod.POST, entity, String.class);
        return response.getBody();
    }
}