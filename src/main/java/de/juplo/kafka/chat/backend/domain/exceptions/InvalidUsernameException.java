package de.juplo.kafka.chat.backend.domain;

import lombok.Getter;


public class InvalidUsernameException extends RuntimeException
{
  @Getter
  private final String username;

  public InvalidUsernameException(String username)
  {
    super("Invalid username: " + username);
    this.username = username;
  }
}
