package com.example.e_rechnung.Erechnung.service.validation;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaValidationService {

    private final SchemaFactory schemaFactory;

    public boolean validateXmlAgainstSchema(String xmlContent) {
        return validateXmlAgainstBytes(xmlContent.getBytes());
    }

    public boolean validateXmlAgainstBytes(byte[] xmlBytes) {
        try {
            Schema schema = loadSchema();
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xmlBytes)));
            return true;
        } catch (SAXException | IOException e) {
            log.error("Validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Schema loadSchema() throws SAXException {
        // تحميل ملف XSD من resources/schemas/xrechnung_3.0.1.xsd
        try (InputStream xsd = new ClassPathResource("schemas/xrechnung_3.0.1.xsd").getInputStream()) {
            return schemaFactory.newSchema(new StreamSource(xsd));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load XSD schema", e);
        }
    }
}