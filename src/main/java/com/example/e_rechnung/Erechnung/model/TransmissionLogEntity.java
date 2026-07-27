package com.example.e_rechnung.Erechnung.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transmission_logs")
@Setter
@Getter
public class TransmissionLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;
    private String peppolMessageId;
    private String status; // SENT, DELIVERED, FAILED
    private String errorDetails;
    private LocalDateTime sentAt;
}