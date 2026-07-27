package com.example.e_rechnung.Erechnung.RestController;

import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import com.example.e_rechnung.Erechnung.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class DownloadController {

    private final InvoiceRepository invoiceRepository;

    /**
     * Download der Rechnung im XML-Format (ursprüngliches XRechnung-Format).
     */
    @GetMapping(value = "/download/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> downloadXml(@PathVariable Long id) {
        InvoiceEntity entity = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rechnung mit ID " + id + " nicht gefunden."));

        // Konvertierung des XML-Strings in Bytes mit UTF-8 Kodierung
        byte[] xmlBytes = entity.getXmlContent().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + entity.getInvoiceNumber() + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlBytes);
    }

    /**
     * Überprüfung des Rechnungsstatus (z. B. gesendet, empfangen, fehlerhaft).
     */

    @GetMapping("/status/{invoiceNumber}")
    public ResponseEntity<InvoiceEntity> getStatus(@PathVariable String invoiceNumber) {
        InvoiceEntity entity = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return ResponseEntity.ok(entity);
    }
}