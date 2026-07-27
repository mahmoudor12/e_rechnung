package com.example.e_rechnung.Erechnung.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateInvoiceRequest {
    @NotBlank
    private String invoiceNumber;

    @NotNull
    private LocalDate issueDate;

    @NotNull
    private LocalDate dueDate;

    @NotBlank
    private String sellerName;

    private String sellerVatId;

    private String sellerEmail;

    @NotBlank
    private String buyerName;

    private String buyerVatId;

    @NotNull
    @Size(min = 1)
    private List<InvoiceItemDto> items;
    private String buyerReference;
    @Data
    public static class InvoiceItemDto {
        @NotBlank
        private String description;
        @NotNull @Positive
        private BigDecimal quantity;
        @NotNull @Positive
        private BigDecimal unitPrice;
        @NotNull @Positive
        private BigDecimal vatRate; // 19, 7, etc
        private String unitCode = "C62"; // default قطعة
    }
}