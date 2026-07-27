package com.example.e_rechnung.Erechnung.util;



import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class GoBDComplianceChecker {
    public boolean isAuditProof(byte[] fileContent, LocalDateTime timestamp) {

        return fileContent != null && timestamp != null;
    }
}