package de.juplo.kafka.chat.backend.implementation.haproxy;

import org.springframework.http.HttpStatusCode;


public class DataPlaneApiErrorException extends RuntimeException
{
  private final HttpStatusCode statusCode;
  private final int errorCode;
  private final String message;


  public DataPlaneApiErrorException(
      HttpStatusCode statusCode,
      DataPlaneApiErrorTo error)
  {
    super(error.message());
    this.statusCode = statusCode;
    this.errorCode = error.code();
    this.message = error.message();
  }

  @Override
  public String toString()
  {
    return statusCode + " - " + errorCode + ": " + message;
  }
}
