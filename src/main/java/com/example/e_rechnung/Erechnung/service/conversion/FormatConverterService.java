package com.example.e_rechnung.Erechnung.service.conversion;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormatConverterService {
    // يمكن تحويل بين XML و PDF أو بين صيغ مختلفة
    public byte[] convertXRechnungToZUGFeRD(byte[] xml) {
        // منطق التحويل باستخدام مكتبات إضافية
        throw new UnsupportedOperationException("Not implemented yet");
    }
}