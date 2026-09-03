package com.example.e_rechnung.Erechnung.dto;

import java.time.LocalDate;

public record InvoiceSummaryDTO(
        Long id,
        String invoiceNumber,
        LocalDate issueDate,
        String sellerName,
        String buyerName,
        String status
) {

}