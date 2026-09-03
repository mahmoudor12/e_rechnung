package com.example.e_rechnung.Erechnung.service.generation;



import com.example.e_rechnung.Erechnung.dto.request.CreateInvoiceRequest;
import com.example.e_rechnung.Erechnung.dto.response.InvoiceResponse;
import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import com.example.e_rechnung.Erechnung.repository.InvoiceRepository;
import com.example.e_rechnung.Erechnung.util.GoBDComplianceChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridGeneratorService {

    private final XRechnungGeneratorService xRechnungGeneratorService;
    private final InvoiceRepository invoiceRepository;
    private final GoBDComplianceChecker goBDComplianceChecker;

    @Transactional
    public InvoiceResponse generateAndStore(CreateInvoiceRequest request,String tenantId) {
        byte[] xml = xRechnungGeneratorService.generate(request);
        boolean auditOk = goBDComplianceChecker.isAuditProof(xml, LocalDateTime.now());
        if (!auditOk) {
            throw new RuntimeException("Generated invoice is not GoBD compliant");
        }

        InvoiceEntity entity = new InvoiceEntity();
        entity.setInvoiceNumber(request.getInvoiceNumber());
        entity.setIssueDate(request.getIssueDate());
        entity.setDueDate(request.getDueDate());
        entity.setSellerName(request.getSellerName());
        entity.setBuyerName(request.getBuyerName());
        entity.setStatus("GENERATED");
        entity.setXmlContent(new String(xml));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setTenantId(tenantId);

        invoiceRepository.save(entity);

        return InvoiceResponse.builder()
                .invoiceNumber(entity.getInvoiceNumber())
                .status("GENERATED")
                .message("Invoice generated and stored")
                .downloadUrl("/api/invoices/download/" +
                        entity.getId())
                .build();
    }
}