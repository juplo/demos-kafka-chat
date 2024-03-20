package de.juplo.kafka.chat.backend;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


@EqualsAndHashCode
@ToString
class User
{
  @Getter
  private final String name;
  private int serial = 0;


  User (String name)
  {
    this.name = name;
  }


  int nextSerial()
  {
    return ++serial;
  }
}