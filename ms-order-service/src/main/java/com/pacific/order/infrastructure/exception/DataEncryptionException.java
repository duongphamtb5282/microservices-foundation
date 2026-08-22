package com.pacific.order.infrastructure.exception;

/**
 * Raised when field-level encryption/decryption fails. Encryption failure must never silently store
 * plaintext in the database, and decryption failure must never surface raw ciphertext as if it were
 * plaintext (review finding: both previously fell back to unencrypted values).
 */
public class DataEncryptionException extends RuntimeException {

  public DataEncryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
