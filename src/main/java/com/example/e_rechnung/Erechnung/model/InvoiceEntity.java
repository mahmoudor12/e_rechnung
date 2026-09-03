package com.example.e_rechnung.Erechnung.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "invoices")
public class InvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String sellerName;
    private String buyerName;
    private String status; // PENDING, SENT, FAILED
    @Column(nullable = false)
    private String tenantId;
    @Column(columnDefinition = "TEXT")
    private String xmlContent;
    @ElementCollection
    private List<String> errors = new ArrayList<>();
    private LocalDateTime createdAt;
}