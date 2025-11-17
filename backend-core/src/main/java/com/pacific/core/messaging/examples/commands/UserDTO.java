package com.pacific.core.messaging.examples.commands;

import java.io.Serializable;

import lombok.Builder;
import lombok.Value;

/** Example DTO for User data. */
@Value
@Builder
public class UserDTO implements Serializable {
  String id;
  String username;
  String email;
}
