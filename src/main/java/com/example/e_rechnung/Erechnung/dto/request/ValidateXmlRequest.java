package com.example.e_rechnung.Erechnung.dto.request;



import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class ValidateXmlRequest {
    @NotBlank
    private String xmlContent; // XML as string or Base64
}