package com.pacific.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Base event class for all domain events */
@Data
@AllArgsConstructor
public abstract class BaseEvent {

  private String eventId;
  private LocalDateTime timestamp;

  private String eventType;
  private String source;
  private String version;

  protected BaseEvent(String eventType, String source, String version) {
    this.eventId = UUID.randomUUID().toString();
    this.timestamp = LocalDateTime.now();
    this.eventType = eventType;
    this.source = source;
    this.version = version;
  }

  // Default constructor for Lombok
  public BaseEvent() {
    this.eventId = UUID.randomUUID().toString();
    this.timestamp = LocalDateTime.now();
  }
}
