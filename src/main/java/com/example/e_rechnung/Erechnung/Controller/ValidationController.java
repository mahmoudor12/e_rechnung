package com.example.e_rechnung.Erechnung.Controller;


import com.example.e_rechnung.Erechnung.dto.request.ValidateXmlRequest;
import com.example.e_rechnung.Erechnung.dto.response.ValidationReport;
import com.example.e_rechnung.Erechnung.service.validation.BusinessValidationService;
import com.example.e_rechnung.Erechnung.service.validation.SchemaValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
public class ValidationController {

    private final SchemaValidationService schemaValidationService;
    private final BusinessValidationService businessValidationService;

    @PostMapping("/schema")
    public ValidationReport validateSchema(@RequestBody ValidateXmlRequest request) {
        boolean valid = schemaValidationService.validateXmlAgainstSchema(request.getXmlContent());
        return ValidationReport.builder()
                .valid(valid)
                .errors(valid ? null : java.util.List.of("Schema validation failed"))
                .build();
    }

    @PostMapping("/business")
    public ValidationReport validateBusiness(@RequestBody ValidateXmlRequest request) {
        return businessValidationService.validateEn16931(request.getXmlContent());
    }
}