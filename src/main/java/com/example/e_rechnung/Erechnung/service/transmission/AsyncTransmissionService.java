package com.example.e_rechnung.Erechnung.service.transmission;

import com.example.e_rechnung.Erechnung.service.transmission.PeppolSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTransmissionService {

    private final PeppolSenderService peppolSenderService;

    @Async
    public void sendInvoiceAsync(String invoiceNumber, byte[] xmlContent, String receiverId) {
        peppolSenderService.sendInvoice(invoiceNumber, xmlContent, receiverId);
    }
}