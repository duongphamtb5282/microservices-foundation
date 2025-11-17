package com.pacific.shared.exceptions.dto;

import lombok.Builder;
import lombok.Data;

/** DTO for field-specific validation errors. */
@Data
@Builder
public class FieldErrorDto {

  /** Field name that has the error */
  private String field;

  /** Rejected value */
  private Object rejectedValue;

  /** Error message for this field */
  private String message;

  /** Error code for this field */
  private String code;
}
