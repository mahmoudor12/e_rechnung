package com.example.e_rechnung.Erechnung.dto.response;


import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    private String invoiceNumber;
    private String status; // GENERATED, SENT, FAILED
    private String message;
    private String downloadUrl;


}