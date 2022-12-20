package de.juplo.kafka.chatroom.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;


@RequiredArgsConstructor
public class Chatroom
{
  @Getter
  private final UUID id;
  @Getter
  private final String name;
  private final List<Message> messages = new LinkedList<>();

  synchronized public Message addMessage(
      UUID id,
      LocalDateTime timestamp,
      String user,
      String text)
  {
    Message message = new Message(id, (long)messages.size(), timestamp, user, text);
    messages.add(message);
    return message;
  }

  public Stream<Message> getMessages(long firstMessage)
  {
    return messages.stream().filter(message -> message.getSerialNumber() >= firstMessage);
  }
}
