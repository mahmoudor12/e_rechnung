package com.example.e_rechnung.Erechnung.service.transmission;





import com.example.e_rechnung.Erechnung.config.PeppolConfig;
import com.example.e_rechnung.Erechnung.exception.PeppolTransmissionException;
import com.example.e_rechnung.Erechnung.model.TransmissionLogEntity;
import com.example.e_rechnung.Erechnung.repository.TransmissionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeppolSenderService {

    private final PeppolConfig peppolConfig;
    private final TransmissionLogRepository transmissionLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendInvoice(String invoiceNumber, byte[] xmlContent, String receiverId) {
        // محاكاة إرسال عبر REST API لمزود خدمة Peppol
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(xmlContent, headers);

        try {
            String url = peppolConfig.getAccessPointUrl() + "/submit?receiver=" + receiverId;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                saveLog(invoiceNumber, response.getBody(), "SENT", null);
                log.info("Invoice {} sent via Peppol", invoiceNumber);
            } else {
                throw new PeppolTransmissionException("Failed to send: " + response.getStatusCode(), null);
            }
        } catch (Exception e) {
            saveLog(invoiceNumber, null, "FAILED", e.getMessage());
            throw new PeppolTransmissionException("Transmission error", e);
        }
    }

    private void saveLog(String invoiceNumber, String messageId, String status, String error) {
        TransmissionLogEntity log = new TransmissionLogEntity();
        log.setInvoiceNumber(invoiceNumber);
        log.setPeppolMessageId(messageId);
        log.setStatus(status);
        log.setErrorDetails(error);
        log.setSentAt(LocalDateTime.now());
        transmissionLogRepository.save(log);
    }
}