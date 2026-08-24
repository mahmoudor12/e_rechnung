package com.example.e_rechnung.Erechnung.Controller;


import com.example.e_rechnung.Erechnung.dto.request.CreateInvoiceRequest;
import com.example.e_rechnung.Erechnung.dto.response.InvoiceResponse;
import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import com.example.e_rechnung.Erechnung.repository.InvoiceRepository;
import com.example.e_rechnung.Erechnung.service.generation.HybridGeneratorService;
import com.example.e_rechnung.Erechnung.service.generation.XRechnungGeneratorService;
import com.example.e_rechnung.Erechnung.service.generation.ZUGFeRDGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InvoiceController {

    private final XRechnungGeneratorService xRechnungGeneratorService;
    private final ZUGFeRDGeneratorService zugFeRdGeneratorService;
    private final HybridGeneratorService hybridGeneratorService;
    private final InvoiceRepository invoiceRepository;

    @PostMapping("/invoices/xrechnung")
    public ResponseEntity<byte[]> createXRechnung(@Valid @RequestBody CreateInvoiceRequest request) {
        byte[] xml = xRechnungGeneratorService.generate(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice.xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @PostMapping("/invoices/zugferd")
    public ResponseEntity<byte[]> createZugferd(@Valid @RequestBody CreateInvoiceRequest request) {
        byte[] pdf = zugFeRdGeneratorService.generateAndValidate(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/invoices/hybrid")
    public ResponseEntity<InvoiceResponse> createHybrid(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = hybridGeneratorService.generateAndStore(request);
        return ResponseEntity.ok(response);
    }



    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceEntity> getInvoiceById(@PathVariable Long id) {
        return invoiceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceEntity>> getAllInvoices() {
        return ResponseEntity.ok(invoiceRepository.findAllByOrderByIdDesc());
    }
}