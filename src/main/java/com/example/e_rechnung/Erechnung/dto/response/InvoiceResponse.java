package com.example.e_rechnung.Erechnung.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceResponse {
    private String invoiceNumber;
    private String status; // GENERATED, SENT, FAILED
    private String message;
    private String downloadUrl;
}