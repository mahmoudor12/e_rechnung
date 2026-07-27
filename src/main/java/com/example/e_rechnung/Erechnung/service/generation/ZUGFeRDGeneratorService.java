package com.example.e_rechnung.Erechnung.service.generation;


import com.example.e_rechnung.Erechnung.dto.request.CreateInvoiceRequest;
import com.example.e_rechnung.Erechnung.service.validation.SchemaValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZUGFeRDGeneratorService {

    private final XRechnungGeneratorService xRechnungGeneratorService;
    private final SchemaValidationService schemaValidationService;

    public byte[] generateAndValidate(CreateInvoiceRequest request) {
        byte[] pdfWithXml = generatePdfWithXml(request);
        // استخراج XML من PDF والتحقق منه - مبسط هنا
        boolean valid = schemaValidationService.validateXmlAgainstBytes(pdfWithXml);
        if (!valid) {
            throw new RuntimeException("Generated PDF fails schema validation");
        }
        log.info("ZUGFeRD PDF generated and validated for invoice: {}", request.getInvoiceNumber());
        return pdfWithXml;
    }

    private byte[] generatePdfWithXml(CreateInvoiceRequest request) {
        // نستخدم نفس بناء Invoice من خدمة XRechnung
        byte[] xml = xRechnungGeneratorService.generate(request);
        // هذا مبسط: في الواقع نحتاج إلى استخدام ZUGFeRDExporterFromA1 مع PDF نفسه
        // لكن لتبسيط المثال، يمكن إرجاع XML فقط، لكن يجب أن يكون PDF حقيقياً.
        // للمثال سنعيد مجرد XML، ولكن الإنتاجي يتطلب PDF/A-3 مع XML مدمج.
        return xml;
    }
}