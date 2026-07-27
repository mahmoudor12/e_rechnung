package com.example.e_rechnung.Erechnung.repository;

import com.example.e_rechnung.Erechnung.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository  extends JpaRepository<AuditLogEntity,Long> {

}
