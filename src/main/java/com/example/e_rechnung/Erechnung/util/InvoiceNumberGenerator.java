package com.example.e_rechnung.Erechnung.util;


import org.springframework.stereotype.Component;
import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InvoiceNumberGenerator {
    private final AtomicLong counter = new AtomicLong(1);

    public String generate() {
        return "INV-" + Year.now().getValue() + "-" + String.format("%06d", counter.getAndIncrement());
    }
}