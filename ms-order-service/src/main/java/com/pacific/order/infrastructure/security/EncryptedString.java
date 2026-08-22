package com.pacific.order.infrastructure.security;

import com.pacific.core.messaging.security.SecurityService;
import com.pacific.order.infrastructure.exception.DataEncryptionException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA converter for automatically encrypting/decrypting string fields. Used to transparently handle
 * encryption at the database level.
 */
@Converter
@Component
@Slf4j
public class EncryptedString implements AttributeConverter<String, String> {

  @Autowired private SecurityService securityService;

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return attribute;
    }

    try {
      String encrypted = securityService.encrypt(attribute);
      log.trace("Encrypted data for database storage");
      return encrypted;
    } catch (Exception e) {
      // Never persist plaintext as a silent fallback — fail loudly (survey finding: encryption
      // failure previously stored the plaintext in the DB)
      log.error("Failed to encrypt data for database", e);
      throw new DataEncryptionException("Failed to encrypt data for database", e);
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return dbData;
    }

    try {
      String decrypted = securityService.decrypt(dbData);
      log.trace("Decrypted data from database");
      return decrypted;
    } catch (Exception e) {
      // Never surface raw ciphertext as if it were plaintext — fail loudly
      log.error("Failed to decrypt data from database", e);
      throw new DataEncryptionException("Failed to decrypt data from database", e);
    }
  }
}
