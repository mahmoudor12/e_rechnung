package com.example.e_rechnung.Erechnung.service.validation;

import de.kosit.validationtool.api.*;
import de.kosit.validationtool.impl.DefaultCheck;
import de.kosit.validationtool.impl.xml.ProcessorProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@Slf4j
public class KositValidationService {

    private Check validator;

    @PostConstruct
    public void init() throws Exception {
        log.info("🚀 Initializing KoSIT Validator...");
        try {
            Path tempDir = Files.createTempDirectory("kosit-config");
            log.info("📁 Temporary config directory: {}", tempDir);

            // Kopiere scenarios.xml
            InputStream scenariosStream = new ClassPathResource("kosit/scenarios.xml").getInputStream();
            Path scenariosPath = tempDir.resolve("kosit/scenarios.xml");
            Files.copy(scenariosStream, scenariosPath, StandardCopyOption.REPLACE_EXISTING);

            // Kopiere rules-Ordner
            Path rulesSource = new ClassPathResource("kosit/rules").getFile().toPath();
            Path rulesTarget = tempDir.resolve("rules");
            copyDirectory(rulesSource, rulesTarget);

            // Lade Konfiguration
            Configuration config = Configuration.load(scenariosPath.toUri()).build(ProcessorProvider.getProcessor());
            this.validator = new DefaultCheck(config);
            log.info("✅ KoSIT Validator initialized successfully.");

        } catch (Exception e) {
            log.error("❌ Failed to initialize KoSIT Validator", e);
            throw new RuntimeException("KoSIT initialization failed", e);
        }
    }

    /**
     * Validiert eine XML-Rechnung mit dem KoSIT-Validator.
     */
    public ValidationResult validate(byte[] xmlContent) {
        log.info("🔍 Validating invoice with KoSIT Validator...");
        try {
            Path tempFile = Files.createTempFile("invoice", ".xml");
            Files.write(tempFile, xmlContent);
            Input input = InputFactory.read(tempFile);

            Result result = validator.checkInput(input);
            boolean isValid = result.isProcessingSuccessful();

            // ✅ Jetzt werden die Fehler korrekt extrahiert
            List<String> errors = new ArrayList<>();
            if (!isValid) {
                errors = extractErrors(result);
                log.warn("Validation failed with {} errors", errors.size());
            }

            Files.deleteIfExists(tempFile);
            return new ValidationResult(isValid, errors);

        } catch (Exception e) {
            log.error("❌ Error during KoSIT validation", e);
            return new ValidationResult(false, List.of("Internal validation error: " + e.getMessage()));
        }
    }

    private void copyDirectory(Path source, Path target) throws Exception {
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to copy directory", e);
            }
        });
    }

    private List<String> extractErrors(Result result) {
        List<String> errors = new ArrayList<>();
        Document report = result.getReportDocument();

        if (report == null) {
            return Collections.singletonList("No validation report generated");
        }

        try {
            XPath xPath = XPathFactory.newInstance().newXPath();

            // ✅ Namespace für SVRL definieren
            xPath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if ("svrl".equals(prefix)) return "http://purl.oclc.org/dsdl/svrl";
                    return null;
                }
                @Override
                public String getPrefix(String namespaceURI) { return null; }
                @Override
                public Iterator<String> getPrefixes(String namespaceURI) { return null; }
            });

            // Suche nach failed-assert
            NodeList failedAsserts = (NodeList) xPath.evaluate(
                    "//svrl:failed-assert", report.getDocumentElement(), XPathConstants.NODESET);

            for (int i = 0; i < failedAsserts.getLength(); i++) {
                String text = failedAsserts.item(i).getTextContent().trim();
                if (!text.isEmpty()) {
                    errors.add(text);
                }
            }

            // Falls keine failed-assert, suche nach messages
            if (errors.isEmpty()) {
                NodeList messages = (NodeList) xPath.evaluate(
                        "//svrl:message", report.getDocumentElement(), XPathConstants.NODESET);
                for (int i = 0; i < messages.getLength(); i++) {
                    String text = messages.item(i).getTextContent().trim();
                    if (!text.isEmpty()) {
                        errors.add(text);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Could not parse validation report: {}", e.getMessage());
            errors.add("Unable to parse validation report: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Ergebnis der KoSIT-Validierung.
     */
    public record ValidationResult(boolean isValid, List<String> errors) {

        /**
         * Gibt eine unveränderliche Liste der Fehler zurück.
         */
        public List<String> getErrors() {
            return errors != null ? List.copyOf(errors) : List.of();
        }

        /**
         * Überprüft, ob die Validierung erfolgreich war.
         */
        public boolean isValid() {
            return isValid && (errors == null || errors.isEmpty());
        }
    }
}