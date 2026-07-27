package com.example.e_rechnung.Erechnung.service.transmission;

import com.example.e_rechnung.Erechnung.model.AuditLogEntity;
import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import com.example.e_rechnung.Erechnung.repository.AuditLogRepository;
import com.example.e_rechnung.Erechnung.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mustangproject.Invoice;
import org.mustangproject.ZUGFeRD.ZUGFeRDInvoiceImporter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeppolReceiverService {

    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Empfängt eine elektronische Rechnung im XML- oder PDF-Format,
     * liest die Stammdaten aus und speichert sie in der Datenbank.
     *
     * @param xmlContent Rohdaten der E-Rechnung (XML oder Hybrid-PDF)
     * @return Die gespeicherte InvoiceEntity
     */
    public InvoiceEntity processAndSaveIncomingInvoice(byte[] xmlContent) {
        log.info("Verarbeite eingehende Rechnung (Größe: {} Bytes)", xmlContent.length);

        try {
            // 1. ZUGFeRD/Factur-X/XRechnung über Mustangproject parsen
            ZUGFeRDInvoiceImporter importer = new ZUGFeRDInvoiceImporter(new ByteArrayInputStream(xmlContent));
            Invoice invoice = importer.extractInvoice();

            // 2. Rechnungsdaten extrahieren
            String invoiceNumber = invoice.getNumber();

            LocalDate issueDate = invoice.getIssueDate() != null ?
                    invoice.getIssueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;

            LocalDate dueDate = invoice.getDueDate() != null ?
                    invoice.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;

            String sellerName = invoice.getSender() != null ? invoice.getSender().getName() : null;
            String buyerName = invoice.getRecipient() != null ? invoice.getRecipient().getName() : null;

            // 3. Entity-Objekt für Datenbank aufbauen
            InvoiceEntity entity = new InvoiceEntity();
            entity.setInvoiceNumber(invoiceNumber);
            entity.setIssueDate(issueDate);
            entity.setDueDate(dueDate);
            entity.setSellerName(sellerName);
            entity.setBuyerName(buyerName);
            entity.setStatus("EMPFANGEN");
            entity.setXmlContent(new String(xmlContent, StandardCharsets.UTF_8));
            entity.setCreatedAt(LocalDateTime.now());

            // 4. In der Datenbank speichern
            InvoiceEntity savedEntity = invoiceRepository.save(entity);
            log.info("✅ Eingehende Rechnung '{}' erfolgreich unter ID {} gespeichert.", invoiceNumber, savedEntity.getId());

            // 5. Audit-Log-Eintrag schreiben
            saveAuditLog(invoiceNumber, "EMPFANGEN", "Über Peppol/Netzwerk empfangen");

            return savedEntity;

        } catch (Exception e) {
            log.error("❌ Fehler beim Verarbeiten der eingehenden Rechnung: {}", e.getMessage(), e);
            throw new RuntimeException("Fehler beim Verarbeiten der eingehenden Rechnung: " + e.getMessage(), e);
        }
    }

    /**
     * Erstellt einen Audit-Eintrag zur Nachvollziehbarkeit (GoBD).
     */
    private void saveAuditLog(String invoiceNumber, String action, String details) {
        try {
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.setAction(action);
            auditLog.setInvoiceId(invoiceNumber);
            auditLog.setDetails(details);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setUserId("SYSTEM");
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("⚠️ Audit-Log konnte nicht gespeichert werden: {}", e.getMessage());
        }
    }
}