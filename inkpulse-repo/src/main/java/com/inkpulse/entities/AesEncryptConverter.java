package com.inkpulse.entities;

import com.inkpulse.crypto.ICryptographyService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class AesEncryptConverter implements AttributeConverter<String, String> {

    private static ICryptographyService cryptoService;

    @Autowired
    public void setCryptoService(ICryptographyService service) {
        AesEncryptConverter.cryptoService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        return cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        return cryptoService.decrypt(dbData);
    }
}
