package com.example.e_rechnung.Erechnung.service.validation;

import de.kosit.validationtool.api.*;
import de.kosit.validationtool.impl.DefaultCheck;
import de.kosit.validationtool.impl.xml.ProcessorProvider;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * Service für die KoSIT-Validierung von XRechnung- und EN16931-konformen Rechnungen.
 * Die Initialisierung erfolgt lazy beim ersten Aufruf, um den Speicherverbrauch beim Start zu reduzieren.
 */
@Service
@Slf4j
public class KositValidationService {

    // Pfade der Ressourcen, die tatsächlich benötigt werden – reduziert die Kopierlast
    private static final List<String> REQUIRED_RESOURCE_PATHS = List.of(
            "kosit/scenarios.xml",
            "kosit/resources/ubl/2.1/xsl/",
            "kosit/resources/ubl/2.1/xsd/maindoc/UBL-Invoice-2.1.xsd",
            "kosit/resources/ubl/2.1/xsd/maindoc/UBL-CreditNote-2.1.xsd",
            "kosit/resources/xrechnung/3.0.2/xsl/",
            "kosit/resources/xrechnung/3.0.2/xsd/",
            "kosit/resources/xrechnung-report.xsl",
            "kosit/resources/default-report.xsl",
            "kosit/resources/cii/16b/xsl/",
            "kosit/resources/cii/16b/xsd/"
            // Weitere Pfade können bei Bedarf ergänzt werden
    );

    private Check validator;
    private Path tempDir;
    private volatile boolean initialized = false;

    /**
     * Stellt sicher, dass der Validator initialisiert ist (lazy).
     * Wird vor jeder Validierung aufgerufen.
     */
    private synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        log.info("🚀 Initializing KoSIT Validator (lazy)...");
        try {
            // 1. Temporäres Verzeichnis erstellen
            this.tempDir = Files.createTempDirectory("kosit-config");
            log.info("📁 Temporary config directory: {}", tempDir);

            // 2. Nur benötigte Ressourcen kopieren
            Path kositRoot = tempDir.resolve("kosit");
            copyRequiredResources(kositRoot);

            // 3. scenarios.xml prüfen
            Path scenariosPath = kositRoot.resolve("scenarios.xml");
            if (!Files.exists(scenariosPath)) {
                throw new IllegalStateException("scenarios.xml not found at: " + scenariosPath.toAbsolutePath());
            }
            log.info("📄 scenarios.xml found, size: {} bytes", Files.size(scenariosPath));

            // 4. Validator bauen
            Configuration config = Configuration.load(scenariosPath.toUri()).build(ProcessorProvider.getProcessor());
            this.validator = new DefaultCheck(config);
            initialized = true;
            log.info("✅ KoSIT Validator initialized successfully.");
        } catch (Exception e) {
            log.error("❌ Failed to initialize KoSIT Validator", e);
            throw new RuntimeException("KoSIT initialization failed", e);
        }
    }

    /**
     * Validiert eine XML-Rechnung mit dem KoSIT-Validator.
     * Die Initialisierung wird bei Bedarf gestartet.
     */
    public ValidationResult validate(byte[] xmlContent) {
        ensureInitialized(); // ← hier wird erstmals initialisiert

        log.info("🔍 Validating invoice with KoSIT Validator...");
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("invoice-", ".xml");
            Files.write(tempFile, xmlContent);

            Input input = InputFactory.read(tempFile);
            Result result = validator.checkInput(input);
            boolean isValid = result.isProcessingSuccessful();

            List<String> errors = new ArrayList<>();
            if (!isValid) {
                errors = extractErrors(result);
                log.warn("Validation failed with {} errors", errors.size());
            } else {
                log.info("✅ Invoice validation successful.");
            }

            return new ValidationResult(isValid, errors);

        } catch (Exception e) {
            log.error("❌ Error during KoSIT validation", e);
            return new ValidationResult(false, List.of("Internal validation error: " + e.getMessage()));
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

    /**
     * Kopiert nur die für die Validierung benötigten Ressourcen aus dem Classpath.
     * Spart Speicher und Zeit gegenüber dem vollständigen Kopieren aller 110 Dateien.
     */
    private void copyRequiredResources(Path targetRoot) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        int copiedCount = 0;

        for (String pathPattern : REQUIRED_RESOURCE_PATHS) {
            // Bei Ordnern müssen wir alle darunter liegenden Dateien kopieren
            if (pathPattern.endsWith("/")) {
                String folderPattern = pathPattern + "**/*";
                Resource[] resources = resolver.getResources("classpath*:" + folderPattern);
                for (Resource resource : resources) {
                    if (!resource.isReadable() || resource.getFilename() == null) {
                        continue;
                    }
                    String relativePath = extractRelativePath(resource, "kosit");
                    if (relativePath == null) {
                        continue;
                    }
                    Path targetFile = targetRoot.resolve(relativePath);
                    Files.createDirectories(targetFile.getParent());
                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        copiedCount++;
                    } catch (Exception e) {
                        log.warn("❌ Failed to copy {}: {}", relativePath, e.getMessage());
                    }
                }
            } else {
                // Einzelne Datei
                Resource resource = resolver.getResource("classpath*:" + pathPattern);
                if (resource.exists() && resource.isReadable()) {
                    String relativePath = extractRelativePath(resource, "kosit");
                    if (relativePath != null) {
                        Path targetFile = targetRoot.resolve(relativePath);
                        Files.createDirectories(targetFile.getParent());
                        try (InputStream is = resource.getInputStream()) {
                            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                            copiedCount++;
                        } catch (Exception e) {
                            log.warn("❌ Failed to copy {}: {}", relativePath, e.getMessage());
                        }
                    }
                }
            }
        }

        log.info("✅ Successfully copied {} required resources from classpath to temporary directory", copiedCount);
    }

    /**
     * Hilfsmethode zum Extrahieren des relativen Pfades aus einer Resource.
     */
    private String extractRelativePath(Resource resource, String rootFolder) {
        try {
            String url = resource.getURL().toString();
            int idx = url.indexOf(rootFolder);
            if (idx == -1) {
                return null;
            }
            String path = url.substring(idx + rootFolder.length());
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } catch (IOException e) {
            log.warn("Could not extract path for resource: {}", resource, e);
            return null;
        }
    }

    /**
     * Extrahiert Fehlermeldungen aus dem Validierungsreport.
     */
    private List<String> extractErrors(Result result) {
        List<String> errors = new ArrayList<>();
        Document report = result.getReportDocument();
        if (report == null) {
            return Collections.singletonList("No validation report generated");
        }

        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if ("svrl".equals(prefix)) return "http://purl.oclc.org/dsdl/svrl";
                    if ("rep".equals(prefix)) return "http://www.xoev.de/de/validator/varl/1";
                    return null;
                }
                @Override
                public String getPrefix(String namespaceURI) { return null; }
                @Override
                public Iterator<String> getPrefixes(String namespaceURI) { return null; }
            });

            // Schematron-Fehler
            NodeList failedAsserts = (NodeList) xPath.evaluate(
                    "//svrl:failed-assert/svrl:text", report.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < failedAsserts.getLength(); i++) {
                String text = failedAsserts.item(i).getTextContent().trim();
                if (!text.isEmpty()) {
                    errors.add(text);
                }
            }

            // XSD-Schema-Fehler (falls keine Schematron-Fehler)
            if (errors.isEmpty()) {
                NodeList messages = (NodeList) xPath.evaluate(
                        "//rep:message", report.getDocumentElement(), XPathConstants.NODESET);
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

    @PreDestroy
    public void cleanup() {
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                deleteDirectoryRecursively(tempDir);
                log.info("🧹 Cleaned up temporary KoSIT directory: {}", tempDir);
            } catch (IOException e) {
                log.warn("⚠️ Could not fully clean up temp dir: {}", e.getMessage());
            }
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Ergebnis der KoSIT-Validierung.
     */
    public record ValidationResult(boolean isValid, List<String> errors) {
        public List<String> getErrors() {
            return errors != null ? List.copyOf(errors) : List.of();
        }
        public boolean isValid() {
            return isValid && (errors == null || errors.isEmpty());
        }
    }
}