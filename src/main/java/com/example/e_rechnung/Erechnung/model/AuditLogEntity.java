package com.example.e_rechnung.Erechnung.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Setter
@Getter
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // GENERATE, SEND, RECEIVE, ARCHIVE
    private String invoiceId;
    private String details;
    private String userId;
    private LocalDateTime timestamp;
}