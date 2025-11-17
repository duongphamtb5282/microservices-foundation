package com.pacific.shared.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.pacific.shared.exceptions.ValidationException;

/** Validation utility class for common validation logic */
public class ValidationUtils {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

  public static void validateEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      throw new ValidationException("Email is required");
    }

    if (!EMAIL_PATTERN.matcher(email).matches()) {
      Map<String, String> errors = new HashMap<>();
      errors.put("email", "Invalid email format");
      throw new ValidationException("Invalid email format", errors);
    }
  }

  public static void validateUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      throw new ValidationException("Username is required");
    }

    if (!USERNAME_PATTERN.matcher(username).matches()) {
      Map<String, String> errors = new HashMap<>();
      errors.put(
          "username",
          "Username must be 3-20 characters long and contain only letters, numbers, and underscores");
      throw new ValidationException("Invalid username format", errors);
    }
  }

  public static void validatePassword(String password) {
    if (password == null || password.trim().isEmpty()) {
      throw new ValidationException("Password is required");
    }

    if (password.length() < 8) {
      Map<String, String> errors = new HashMap<>();
      errors.put("password", "Password must be at least 8 characters long");
      throw new ValidationException("Password must be at least 8 characters long", errors);
    }
  }
}
