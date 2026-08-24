package com.example.e_rechnung.Erechnung.repository;


import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber);

    @Nullable List<InvoiceEntity> findAllByOrderByIdDesc();
}