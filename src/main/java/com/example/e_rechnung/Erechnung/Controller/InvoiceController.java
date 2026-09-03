package com.example.e_rechnung.Erechnung.Controller;

import com.example.e_rechnung.Erechnung.dto.InvoiceSummaryDTO;
import com.example.e_rechnung.Erechnung.dto.request.CreateInvoiceRequest;
import com.example.e_rechnung.Erechnung.dto.response.InvoiceResponse;
import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import com.example.e_rechnung.Erechnung.repository.InvoiceRepository;
import com.example.e_rechnung.Erechnung.service.generation.HybridGeneratorService;
import com.example.e_rechnung.Erechnung.service.generation.XRechnungGeneratorService;
import com.example.e_rechnung.Erechnung.service.generation.ZUGFeRDGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.AccessDeniedException;

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

    // --- @AuthenticationPrincipal Jwt jwt wurde zu allen Methoden hinzugefügt ---


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
    public ResponseEntity<InvoiceResponse> createHybrid(@Valid @RequestBody CreateInvoiceRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaim("org_id");
        InvoiceResponse response = hybridGeneratorService.generateAndStore(request, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceEntity> getInvoiceById(@PathVariable Long id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaim("org_id");
        // Suche nach der Rechnung unter Berücksichtigung der tenantId, um strikte Isolation zu gewährleisten
        return invoiceRepository.findByIdAndTenantId(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*@GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceEntity>> getAllInvoices(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        String tenantId = jwt.getClaim("org_id");
        if (tenantId == null) {
            throw new AccessDeniedException("User does not belong to any organization");
        }

        Page<InvoiceEntity> page = invoiceRepository.findAllByTenantId(tenantId, pageable);
        return ResponseEntity.ok(page);
    }*/
    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceSummaryDTO>> getAllInvoices(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        String tenantId = jwt.getClaim("org_id");
        if (tenantId == null) {
            throw new AccessDeniedException("User does not belong to any organization");
        }

        //
        Page<InvoiceSummaryDTO> page = invoiceRepository.findSummaryByTenantId(tenantId, pageable);
        return ResponseEntity.ok(page);
    }
}