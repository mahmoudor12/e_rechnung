package com.example.e_rechnung.Erechnung.RestController;

import com.example.e_rechnung.Erechnung.service.transmission.AsyncInvoiceProcessor;
import com.example.e_rechnung.Erechnung.service.validation.KositValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST-Controller für den Empfang von eingehenden Rechnungen (Inbound).
 * Empfängt Rechnungen über Peppol-Webhooks oder andere Quellen.
 */
@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")  // CORS – bei Bedarf anpassen
public class InboundInvoiceController {

    private final KositValidationService kositValidationService;
    private final AsyncInvoiceProcessor asyncInvoiceProcessor;

    // Maximale XML-Größe (z. B. 10 MB)
    private static final int MAX_XML_SIZE = 10 * 1024 * 1024;

    /**
     * Haupt-Endpunkt zum Empfangen von Rechnungen aus dem Peppol-Netzwerk.
     * Der Peppol-Provider sendet den XML-Inhalt der Rechnung an diesen Endpunkt.
     * Die eigentliche Verarbeitung (Speicherung, Archivierung) erfolgt asynchron,
     * um Timeouts zu vermeiden.
     *
     * @param xmlContent Der XML-Inhalt der Rechnung (XRechnung oder ZUGFeRD)
     * @return 202 ACCEPTED mit einer Callback-ID für den Statusabruf
     */
    @PostMapping(value = "/peppol", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<Map<String, String>> receivePeppolInvoice(@RequestBody byte[] xmlContent) {
        log.info("📥 Eingehende Rechnung über Peppol-Webhook empfangen. Größe: {} Bytes", xmlContent.length);

        // 1. Prüfen, ob die Anfrage leer ist
        if (xmlContent == null || xmlContent.length == 0) {
            log.warn("Leere Anfrage erhalten.");
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "Leere Anfrage – keine XML-Daten enthalten"
            ));
        }

        // 2. Größenprüfung (optional, aber sicherheitsrelevant)
        if (xmlContent.length > MAX_XML_SIZE) {
            log.warn("XML-Größe {} überschreitet das Limit von {} Bytes", xmlContent.length, MAX_XML_SIZE);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "XML-Daten zu groß",
                    "details", "Maximal erlaubt: " + MAX_XML_SIZE + " Bytes"
            ));
        }

        // 3. KoSIT-Validierung (vollständige Prüfung: XSD + Schematron + EN 16931)
        KositValidationService.ValidationResult result = kositValidationService.validate(xmlContent);

        if (!result.isValid()) {
            List<String> errors = result.getErrors();
            log.error("❌ KoSIT-Validierung fehlgeschlagen: {}", errors);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "Validierung fehlgeschlagen",
                    "details", String.join("; ", errors)
            ));
        }

        // 4. Generiere eine eindeutige ID für den asynchronen Verarbeitungsauftrag
        String callbackId = UUID.randomUUID().toString();

        // 5. Starte die asynchrone Verarbeitung (Speicherung + Archivierung)
        try {
            asyncInvoiceProcessor.processInvoiceAsync(xmlContent, callbackId);
            log.info("✅ Rechnung zur asynchronen Verarbeitung angemeldet. CallbackId: {}", callbackId);
            return ResponseEntity.accepted().body(Map.of(
                    "status", "ACCEPTED",
                    "callbackId", callbackId,
                    "message", "Rechnung wurde zur Verarbeitung angenommen. Status kann später abgerufen werden."
            ));
        } catch (Exception e) {
            log.error("🔥 Fehler beim Starten der asynchronen Verarbeitung: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Interner Fehler beim Starten der Verarbeitung",
                            "details", e.getMessage()
                    ));
        }
    }

    /**
     * Zusätzlicher Endpunkt zum Empfangen von Rechnungen im Multipart-Format (z.B. für Testzwecke).
     * Leitet die Anfrage an den Haupt-Endpunkt weiter.
     */
    @PostMapping(value = "/peppol/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> receivePeppolInvoiceMultipart(@RequestParam("file") byte[] xmlContent) {
        return receivePeppolInvoice(xmlContent);
    }

    /**
     * Health-Check für den Load-Balancer (z. B. AWS ALB).
     * Kann auch über Spring Actuator realisiert werden.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "inbound-controller"
        ));
    }
}