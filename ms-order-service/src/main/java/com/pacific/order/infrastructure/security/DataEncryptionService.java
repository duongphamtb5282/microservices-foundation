package com.pacific.order.infrastructure.security;

import com.pacific.core.messaging.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for encrypting/decrypting sensitive data in the Order Service.
 * Handles customer data, payment information, and other sensitive fields.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataEncryptionService {

    private final SecurityService securityService;

    /**
     * Encrypt customer email address.
     */
    public String encryptEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        return securityService.encrypt(email);
    }

    /**
     * Decrypt customer email address.
     */
    public String decryptEmail(String encryptedEmail) {
        if (encryptedEmail == null || encryptedEmail.isEmpty()) {
            return encryptedEmail;
        }
        return securityService.decrypt(encryptedEmail);
    }

    /**
     * Encrypt customer phone number.
     */
    public String encryptPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        return securityService.encrypt(phone);
    }

    /**
     * Decrypt customer phone number.
     */
    public String decryptPhone(String encryptedPhone) {
        if (encryptedPhone == null || encryptedPhone.isEmpty()) {
            return encryptedPhone;
        }
        return securityService.decrypt(encryptedPhone);
    }

    /**
     * Encrypt shipping address.
     */
    public String encryptAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        return securityService.encrypt(address);
    }

    /**
     * Decrypt shipping address.
     */
    public String decryptAddress(String encryptedAddress) {
        if (encryptedAddress == null || encryptedAddress.isEmpty()) {
            return encryptedAddress;
        }
        return securityService.decrypt(encryptedAddress);
    }

    /**
     * Encrypt payment method details (card number, etc.).
     */
    public String encryptPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            return paymentMethod;
        }
        return securityService.encrypt(paymentMethod);
    }

    /**
     * Decrypt payment method details.
     */
    public String decryptPaymentMethod(String encryptedPaymentMethod) {
        if (encryptedPaymentMethod == null || encryptedPaymentMethod.isEmpty()) {
            return encryptedPaymentMethod;
        }
        return securityService.decrypt(encryptedPaymentMethod);
    }

    /**
     * Hash customer identifier for analytics (one-way hash).
     */
    public String hashCustomerId(String customerId) {
        if (customerId == null || customerId.isEmpty()) {
            return customerId;
        }
        return securityService.hashData("customer:" + customerId);
    }

    /**
     * Encrypt any sensitive string data.
     */
    public String encryptSensitiveData(String data, String dataType) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        log.debug("Encrypting sensitive data of type: {}", dataType);
        return securityService.encrypt(data);
    }

    /**
     * Decrypt any sensitive string data.
     */
    public String decryptSensitiveData(String encryptedData, String dataType) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        log.debug("Decrypting sensitive data of type: {}", dataType);
        return securityService.decrypt(encryptedData);
    }

    /**
     * Check if data is encrypted (has encryption markers).
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        // Check if data is Base64 encoded (encrypted data is Base64)
        try {
            java.util.Base64.getDecoder().decode(data);
            return true; // Successfully decoded Base64, likely encrypted
        } catch (IllegalArgumentException e) {
            return false; // Not Base64, not encrypted
        }
    }

    /**
     * Sanitize data for logging (mask sensitive information).
     */
    public String sanitizeForLogging(String data, String dataType) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        if (isEncrypted(data)) {
            return "***ENCRYPTED***"; // Don't log encrypted data
        }

        // Mask sensitive data types
        switch (dataType.toLowerCase()) {
            case "email":
                return maskEmail(data);
            case "phone":
                return maskPhone(data);
            case "payment":
                return maskPaymentMethod(data);
            default:
                return data.length() > 4 ? data.substring(0, 2) + "***" + data.substring(data.length() - 2) : "***";
        }
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length == 2) {
            String username = parts[0];
            String domain = parts[1];
            String maskedUsername = username.length() > 2 ?
                username.substring(0, 1) + "*".repeat(username.length() - 2) + username.substring(username.length() - 1) :
                "*".repeat(username.length());
            return maskedUsername + "@" + domain;
        }
        return "***";
    }

    private String maskPhone(String phone) {
        // Remove all non-digits
        String digitsOnly = phone.replaceAll("[^0-9]", "");

        if (digitsOnly.length() >= 4) {
            return "*".repeat(digitsOnly.length() - 4) + digitsOnly.substring(digitsOnly.length() - 4);
        }
        return "*".repeat(digitsOnly.length());
    }

    private String maskPaymentMethod(String paymentMethod) {
        // Mask payment method showing only last 4 characters
        if (paymentMethod.length() >= 4) {
            return "*".repeat(paymentMethod.length() - 4) + paymentMethod.substring(paymentMethod.length() - 4);
        }
        return "*".repeat(paymentMethod.length());
    }
}
