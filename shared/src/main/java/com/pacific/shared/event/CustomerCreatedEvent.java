package com.pacific.shared.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType")
@JsonTypeName("CustomerCreatedEvent")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerCreatedEvent implements Serializable, DomainEvent {

  private static final long serialVersionUID = 1L;

  // Event payload
  private Long customerId;
  private String name;
  private String email;
  private String phone;
  private String address;

  // Correlation and tracing
  @Builder.Default private String correlationId = UUID.randomUUID().toString();

  // Event metadata
  @Builder.Default private String eventType = "CUSTOMER_CREATED";

  @Builder.Default private String version = "1.0";

  @Builder.Default private String sourceService = "customer-service";

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  // Distributed tracing support (optional)
  private String traceId;
  private String spanId;
  private Integer spanSequence;
  private Integer spanCount;

  // Business metadata
  private Double totalValue;
  private String currency;

  @Override
  public String getAggregateId() {
    return customerId != null ? customerId.toString() : correlationId;
  }

  @Override
  public String getEventId() {
    return correlationId + "-" + timestamp.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CustomerCreatedEvent that = (CustomerCreatedEvent) o;
    return Objects.equals(customerId, that.customerId)
        && Objects.equals(correlationId, that.correlationId)
        && Objects.equals(eventType, that.eventType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerId, correlationId, eventType);
  }

  // Validation methods
  public boolean isValid() {
    return customerId != null
        && name != null
        && !name.trim().isEmpty()
        && email != null
        && !email.trim().isEmpty()
        && correlationId != null
        && !correlationId.trim().isEmpty();
  }

  public void validate() {
    if (!isValid()) {
      throw new IllegalArgumentException("CustomerCreatedEvent is not valid: " + this);
    }
  }

  // Convenience methods
  public CustomerCreatedEvent withCorrelationId(String correlationId) {
    this.correlationId =
        correlationId != null && !correlationId.trim().isEmpty()
            ? correlationId.trim()
            : UUID.randomUUID().toString();
    return this;
  }

  public CustomerCreatedEvent withTraceId(String traceId) {
    this.traceId = traceId;
    return this;
  }

  // Builder customization for correlation ID
  public static class CustomerCreatedEventBuilder {
    private String correlationId;
    private Long customerId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String eventType;
    private String version;
    private String sourceService;
    private LocalDateTime timestamp;
    private String traceId;
    private String spanId;
    private Integer spanSequence;
    private Integer spanCount;
    private Double totalValue;
    private String currency;

    public CustomerCreatedEventBuilder correlationId(String correlationId) {
      if (correlationId != null && !correlationId.trim().isEmpty()) {
        this.correlationId = correlationId.trim();
      } else {
        this.correlationId = UUID.randomUUID().toString();
      }
      return this;
    }

    public CustomerCreatedEvent build() {
      // Ensure correlationId is set
      if (this.correlationId == null || this.correlationId.trim().isEmpty()) {
        this.correlationId = UUID.randomUUID().toString();
      }
      // Create the event with all the builder values
      CustomerCreatedEvent event = new CustomerCreatedEvent();
      event.customerId = this.customerId;
      event.name = this.name;
      event.email = this.email;
      event.phone = this.phone;
      event.address = this.address;
      event.correlationId = this.correlationId;
      event.eventType = this.eventType != null ? this.eventType : "CUSTOMER_CREATED";
      event.version = this.version != null ? this.version : "1.0";
      event.sourceService = this.sourceService != null ? this.sourceService : "customer-service";
      event.timestamp = this.timestamp != null ? this.timestamp : LocalDateTime.now();
      event.traceId = this.traceId;
      event.spanId = this.spanId;
      event.spanSequence = this.spanSequence;
      event.spanCount = this.spanCount;
      event.totalValue = this.totalValue;
      event.currency = this.currency;
      return event;
    }
  }
}

// Domain event interface
interface DomainEvent extends Serializable {
  String getAggregateId();

  String getEventId();

  default String getEventType() {
    return this.getClass().getSimpleName();
  }

  default boolean isValid() {
    return true;
  }

  default void validate() {
    if (!isValid()) {
      throw new IllegalArgumentException("Invalid domain event: " + this);
    }
  }
}
