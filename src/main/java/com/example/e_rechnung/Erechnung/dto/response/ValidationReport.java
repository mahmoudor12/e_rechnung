package com.example.e_rechnung.Erechnung.dto.response;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ValidationReport {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
}
