package com.example.e_rechnung.Erechnung.service.transmission;

import com.example.e_rechnung.Erechnung.model.AuditLogEntity;
import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import com.example.e_rechnung.Erechnung.repository.AuditLogRepository;
import com.example.e_rechnung.Erechnung.repository.InvoiceRepository;
import com.example.e_rechnung.Erechnung.service.validation.KositValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeppolReceiverService {

    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final KositValidationService kositValidationService;

    public InvoiceEntity processAndSaveIncomingInvoice(byte[] xmlContent) {
        log.info("Verarbeite eingehende Rechnung (Größe: {} Bytes)", xmlContent.length);

        // 1️⃣ KoSIT-Validierung
        KositValidationService.ValidationResult validationResult = kositValidationService.validate(xmlContent);
        List<String> errors = new ArrayList<>(validationResult.getErrors());

        // 2️⃣ XPath-Parsing + CustomizationID-Prüfung
        InvoiceEntity entity = new InvoiceEntity();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent));

            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new InvoiceNamespaceContext());

            // 🔥 CustomizationID prüfen
            String customizationId = (String) xPath.evaluate("//cbc:CustomizationID[1]/text()", doc, XPathConstants.STRING);
            if (customizationId != null && !customizationId.equals("urn:xeinkauf.de:xrechnung:3.0.2")) {
                errors.add("Ungültige CustomizationID: " + customizationId + " (erwartet: urn:xeinkauf.de:xrechnung:3.0.2)");
                log.warn("⚠️ Ungültige CustomizationID: {}", customizationId);
            }

            // Daten extrahieren
            String invoiceNumber = (String) xPath.evaluate("//cbc:ID[1]/text()", doc, XPathConstants.STRING);
            String issueDateStr = (String) xPath.evaluate("//cbc:IssueDate[1]/text()", doc, XPathConstants.STRING);
            String dueDateStr = (String) xPath.evaluate("//cbc:DueDate[1]/text()", doc, XPathConstants.STRING);
            String sellerName = (String) xPath.evaluate("//cac:AccountingSupplierParty/cac:Party/cac:PartyName/cbc:Name[1]/text()", doc, XPathConstants.STRING);
            String buyerName = (String) xPath.evaluate("//cac:AccountingCustomerParty/cac:Party/cac:PartyName/cbc:Name[1]/text()", doc, XPathConstants.STRING);

            // Falls CII-Format erkannt wird
            if (invoiceNumber == null || invoiceNumber.isEmpty()) {
                xPath.setNamespaceContext(new CIINamespaceContext());
                invoiceNumber = (String) xPath.evaluate("//ram:ID[1]/text()", doc, XPathConstants.STRING);
                sellerName = (String) xPath.evaluate("//ram:SellerTradeParty/ram:Name[1]/text()", doc, XPathConstants.STRING);
                buyerName = (String) xPath.evaluate("//ram:BuyerTradeParty/ram:Name[1]/text()", doc, XPathConstants.STRING);
                String issueDateTime = (String) xPath.evaluate("//ram:IssueDateTime/udt:DateTimeString/text()", doc, XPathConstants.STRING);
                if (issueDateTime != null && !issueDateTime.isEmpty()) {
                    issueDateStr = issueDateTime.substring(0, 8);
                }
            }

            // Entity befüllen
            entity.setInvoiceNumber(invoiceNumber != null ? invoiceNumber : "UNKNOWN-" + System.currentTimeMillis());
            entity.setIssueDate(issueDateStr != null && !issueDateStr.isEmpty() ? LocalDate.parse(issueDateStr) : null);
            entity.setDueDate(dueDateStr != null && !dueDateStr.isEmpty() ? LocalDate.parse(dueDateStr) : null);
            entity.setSellerName(sellerName);
            entity.setBuyerName(buyerName);
            entity.setXmlContent(new String(xmlContent, StandardCharsets.UTF_8));
            entity.setCreatedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("❌ XPath-Parsing Fehler: {}", e.getMessage());
            entity.setInvoiceNumber("UNKNOWN-" + System.currentTimeMillis());
            entity.setXmlContent(new String(xmlContent, StandardCharsets.UTF_8));
            entity.setCreatedAt(LocalDateTime.now());
            errors.add("XPath-Parser-Fehler: " + e.getMessage());
        }

        // 3️⃣ Fehler speichern
        entity.setErrors(errors);
        entity.setStatus(errors.isEmpty() ? "EMPFANGEN" : "EMPFANGEN_MIT_FEHLERN");

        // 4️⃣ Speichern
        InvoiceEntity savedEntity = invoiceRepository.save(entity);
        log.info("✅ Rechnung '{}' gespeichert ({} Fehler).", savedEntity.getInvoiceNumber(), errors.size());

        // 5️⃣ Audit-Log
        saveAuditLog(entity.getInvoiceNumber(), entity.getStatus(),
                errors.isEmpty() ? "Erfolgreich empfangen" : "Empfangen mit " + errors.size() + " Fehlern");

        return savedEntity;
    }

    // Hilfsklassen für Namespaces (unverändert)
    private static class InvoiceNamespaceContext implements javax.xml.namespace.NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            switch (prefix) {
                case "cbc": return "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
                case "cac": return "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
                default: return null;
            }
        }
        @Override
        public String getPrefix(String namespaceURI) { return null; }
        @Override
        public Iterator<String> getPrefixes(String namespaceURI) { return null; }
    }

    private static class CIINamespaceContext implements javax.xml.namespace.NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            switch (prefix) {
                case "rsm": return "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100";
                case "ram": return "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100";
                case "udt": return "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100";
                default: return null;
            }
        }
        @Override
        public String getPrefix(String namespaceURI) { return null; }
        @Override
        public Iterator<String> getPrefixes(String namespaceURI) { return null; }
    }

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