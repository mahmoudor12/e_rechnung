package com.example.e_rechnung.Erechnung.service.status;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class InvoiceStatusService {

    // Einfache In-Memory-Speicherung – für Produktion eher eine Datenbanktabelle
    private final ConcurrentHashMap<String, StatusInfo> statusMap = new ConcurrentHashMap<>();

    public void updateStatus(String callbackId, String status, String detail) {
        statusMap.put(callbackId, new StatusInfo(status, detail, System.currentTimeMillis()));
    }

    public StatusInfo getStatus(String callbackId) {
        return statusMap.get(callbackId);
    }

    public record StatusInfo(String status, String detail, long timestamp) {}
}