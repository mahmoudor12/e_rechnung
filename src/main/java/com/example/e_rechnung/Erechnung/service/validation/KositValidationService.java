package com.example.e_rechnung.Erechnung.service.validation;

import de.kosit.validationtool.api.*;
import de.kosit.validationtool.impl.DefaultCheck;
import de.kosit.validationtool.impl.xml.ProcessorProvider;
import jakarta.annotation.PostConstruct;
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

@Service
@Slf4j
public class KositValidationService {

    private Check validator;
    private Path tempDir;

    @PostConstruct
    public void init() throws Exception {
        log.info("🚀 Initializing KoSIT Validator...");
        try {
            // 1. مجلد مؤقت جديد
            this.tempDir = Files.createTempDirectory("kosit-config");
            log.info("📁 Temporary config directory: {}", tempDir);

            // 2. إنشاء المجلد kosit داخل المؤقت
            Path kositRoot = tempDir.resolve("kosit");

            // 3. نسخ كل الملفات من classpath:kosit/ إلى المجلد المؤقت
            copyClasspathDirectory("kosit", kositRoot);

            // 4. التحقق من وجود scenarios.xml
            Path scenariosPath = kositRoot.resolve("scenarios.xml");
            if (!Files.exists(scenariosPath)) {
                throw new IllegalStateException("scenarios.xml was not found at: " + scenariosPath.toAbsolutePath());
            }
            log.info("📄 scenarios.xml found, size: {} bytes", Files.size(scenariosPath));

            // 5. تحميل التهيئة وبناء المدقق
            Configuration config = Configuration.load(scenariosPath.toUri()).build(ProcessorProvider.getProcessor());
            this.validator = new DefaultCheck(config);
            log.info("✅ KoSIT Validator initialized successfully.");

        } catch (Exception e) {
            log.error("❌ Failed to initialize KoSIT Validator", e);
            throw new RuntimeException("KoSIT initialization failed", e);
        }
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

    /**
     * Validiert eine XML-Rechnung mit dem KoSIT-Validator.
     */
    public ValidationResult validate(byte[] xmlContent) {
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
                    // Ignorieren
                }
            }
        }
    }

    /**
     * Kopiert komplette Ordnerstrukturen aus dem Classpath ins Zielverzeichnis.
     */
    private void copyClasspathDirectory(String sourceFolder, Path targetDir) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:" + sourceFolder + "/**");

        log.info("📂 Found {} resources in classpath under '{}'", resources.length, sourceFolder);

        int fileCount = 0;
        for (Resource resource : resources) {
            if (!resource.isReadable()) {
                continue;
            }

            String urlString = resource.getURL().toString();
            int rootIndex = urlString.indexOf(sourceFolder);
            if (rootIndex == -1) {
                continue;
            }

            String relativePath = urlString.substring(rootIndex + sourceFolder.length());
            if (relativePath.isEmpty() || relativePath.endsWith("/")) {
                continue;
            }

            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            Path targetFile = targetDir.resolve(relativePath);
            Files.createDirectories(targetFile.getParent());

            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                fileCount++;
                // يمكن إلغاء التعليق لرؤية كل ملف
                // log.debug("✅ Copied: {}", relativePath);
            } catch (Exception e) {
                log.warn("❌ Failed to copy {}: {}", relativePath, e.getMessage());
            }
        }

        log.info("✅ Successfully copied {} files from '{}' to temporary directory", fileCount, sourceFolder);

        // تحقق إضافي: هل يوجد مجلد maindoc؟
        Path maindocPath = targetDir.resolve("resources/ubl/2.1/xsd/maindoc");
        if (Files.exists(maindocPath)) {
            log.info("✅ maindoc directory exists at: {}", maindocPath);
            try (Stream<Path> files = Files.list(maindocPath)) {
                files.forEach(f -> log.info("  📄 {}", f.getFileName()));
            }
        } else {
            log.warn("❌ maindoc directory NOT found at: {}", maindocPath);
        }
    }

    /**
     * Extrahiert Fehlermeldungen aus dem Validierungsbericht.
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

            // XSD-Schema-Fehler
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