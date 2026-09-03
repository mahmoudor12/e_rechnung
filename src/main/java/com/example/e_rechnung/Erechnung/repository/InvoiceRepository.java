package com.example.e_rechnung.Erechnung.repository;


import com.example.e_rechnung.Erechnung.dto.InvoiceSummaryDTO;
import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber,String tenantId);

    @Nullable List<InvoiceEntity> findAllByOrderByIdDesc(String tenantId);

    Optional<InvoiceEntity> findByIdAndTenantId(Long id, String tenantId);

    List<InvoiceEntity> findAllByTenantIdOrderByIdDesc(String tenantId);

    Page<InvoiceEntity> findAllByTenantId(String tenantId, Pageable pageable);
    @Query("SELECT new com.example.e_rechnung.Erechnung.dto.InvoiceSummaryDTO(" +
            "i.id, i.invoiceNumber, i.issueDate, i.sellerName, i.buyerName, i.status) " +
            "FROM InvoiceEntity i " +
            "WHERE i.tenantId = :tenantId")
    Page<InvoiceSummaryDTO> findSummaryByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
}