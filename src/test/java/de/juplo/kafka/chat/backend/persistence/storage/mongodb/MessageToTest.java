package de.juplo.kafka.chat.backend.persistence.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.Message;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


public class MessageToTest
{
  @Test
  void testFrom()
  {
    Message message = new Message(
        Message.MessageKey.of("ute", 1l),
        6l,
        LocalDateTime.now(),
        "foo");
    MessageTo messageTo = MessageTo.from(message);
    assertThat(messageTo.getId()).isEqualTo("ute--1");
  }

  @Test
  void testToMessage()
  {
    MessageTo messageTo = new MessageTo(
        "ute--1",
        6l,
        LocalDateTime.now().toString(),
        "foo");
    Message message = messageTo.toMessage();
    assertThat(message.getId()).isEqualTo(1l);
    assertThat(message.getUsername()).isEqualTo("ute");
  }
}
