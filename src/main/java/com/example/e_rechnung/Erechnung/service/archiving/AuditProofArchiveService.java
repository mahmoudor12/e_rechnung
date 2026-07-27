package com.example.e_rechnung.Erechnung.service.archiving;


import com.example.e_rechnung.Erechnung.model.AuditLogEntity;
import com.example.e_rechnung.Erechnung.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditProofArchiveService {

    private final AuditLogRepository auditLogRepository;

    public void archive(String invoiceId, byte[] data, String action) {
        // في الحقيقة يجب تخزين الملف مع توقيع زمني في مكان آمن (S3 أو قرص)
        // وتسجيل عملية الأرشفة
        AuditLogEntity log = new AuditLogEntity();
        log.setAction(action);
        log.setInvoiceId(invoiceId);
        log.setDetails("Archived invoice size: " + data.length);
        log.setUserId("system");
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
