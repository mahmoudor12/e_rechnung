package com.example.e_rechnung.Erechnung.service.transmission;

import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import com.example.e_rechnung.Erechnung.service.archiving.AuditProofArchiveService;
import com.example.e_rechnung.Erechnung.service.validation.KositValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncInvoiceProcessor {

    private final KositValidationService kositValidationService;
    private final PeppolReceiverService peppolReceiverService;
    private final AuditProofArchiveService auditProofArchiveService;

    /**
     * Asynchrone Verarbeitung einer eingehenden Rechnung.
     * Umfasst: Validierung, Speicherung und Archivierung.
     */
    @Async
    @Transactional
    public void processInvoiceAsync(byte[] xmlContent, String callbackId) {
        log.info("🔄 Starte asynchrone Rechnungsverarbeitung (CallbackID: {})", callbackId);

        try {
            // 1. Validierung mittels KoSIT
            KositValidationService.ValidationResult validationResult = kositValidationService.validate(xmlContent);
            if (!validationResult.isValid()) {
                log.error("❌ Validierung fehlgeschlagen für CallbackID {}: {}", callbackId, validationResult.errors());
                // Hier könnte eine Fehlerbenachrichtigung per Webhook oder E-Mail gesendet werden
                return;
            }

            // 2. Speichern der Rechnung in der Datenbank
            InvoiceEntity savedEntity = peppolReceiverService.processAndSaveIncomingInvoice(xmlContent);
            log.info("✅ Rechnung {} erfolgreich gespeichert (CallbackID: {})", savedEntity.getInvoiceNumber(), callbackId);

            // 3. Revisionssichere Archivierung der Rechnung (GoBD-konform)
            auditProofArchiveService.archive(
                    savedEntity.getInvoiceNumber(),
                    xmlContent,
                    "RECEIVED_FROM_PEPPOL"
            );
            log.info("📦 Rechnung {} archiviert (CallbackID: {})", savedEntity.getInvoiceNumber(), callbackId);

            // 4. Hier kann eine Erfolgsbenachrichtigung an das externe System gesendet werden
            // ...

        } catch (Exception e) {
            log.error("🔥 Fehler bei der asynchronen Rechnungsverarbeitung (CallbackID: {}): {}", callbackId, e.getMessage(), e);
            // Fehlerprotokollierung und Senden einer Benachrichtigung zur manuellen Nachbearbeitung
        }
    }
}