package com.pacific.shared.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** User-related events */
public class UserEvent {

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class UserCreated extends BaseEvent {
    private String userId;
    private String username;
    private String email;
    private String role;

    public UserCreated() {
      super("UserCreated", "auth-service", "1.0");
    }

    public UserCreated(String userId, String username, String email, String role) {
      super("UserCreated", "auth-service", "1.0");
      this.userId = userId;
      this.username = username;
      this.email = email;
      this.role = role;
    }
  }

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class UserUpdated extends BaseEvent {
    private String userId;
    private String username;
    private String email;
    private String role;

    public UserUpdated() {
      super("UserUpdated", "auth-service", "1.0");
    }

    public UserUpdated(String userId, String username, String email, String role) {
      super("UserUpdated", "auth-service", "1.0");
      this.userId = userId;
      this.username = username;
      this.email = email;
      this.role = role;
    }
  }

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class UserDeleted extends BaseEvent {
    private String userId;
    private String username;

    public UserDeleted() {
      super("UserDeleted", "auth-service", "1.0");
    }

    public UserDeleted(String userId, String username) {
      super("UserDeleted", "auth-service", "1.0");
      this.userId = userId;
      this.username = username;
    }
  }
}
