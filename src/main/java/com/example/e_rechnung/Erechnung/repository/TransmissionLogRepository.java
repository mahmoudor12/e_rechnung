package com.example.e_rechnung.Erechnung.repository;


import com.example.e_rechnung.Erechnung.model.TransmissionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransmissionLogRepository extends JpaRepository<TransmissionLogEntity, Long> {
}