package com.example.e_rechnung.Erechnung.service.validation;

import com.example.e_rechnung.Erechnung.dto.response.ValidationReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessValidationService {

    public ValidationReport validateEn16931(String xmlContent) {
        List<String> errors = new ArrayList<>();
        // مثال: تحقق من وجود عناصر إلزامية مثل رقم الفاتورة، تاريخ، بائع، مشتري.
        if (!xmlContent.contains("<ram:InvoiceID>")) {
            errors.add("Missing Invoice ID");
        }
        if (!xmlContent.contains("<ram:IssueDateTime>")) {
            errors.add("Missing Issue Date");
        }
        // في الإنتاج يجب استخدام مكتبة Kosit Validator أو التحقق من XSD وقواعد إضافية.
        boolean valid = errors.isEmpty();
        return ValidationReport.builder()
                .valid(valid)
                .errors(valid ? null : errors)
                .build();
    }
}