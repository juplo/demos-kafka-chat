package de.juplo.kafka.chat.backend.persistence.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@EqualsAndHashCode(of = { "id" })
@ToString(of = { "id", "name" })
@Document
public class ChatRoomTo
{
  @Id
  private String id;
  private String name;
  private List<MessageTo> messages;

  public static ChatRoomTo from(ChatRoom chatroom)
  {
    return new ChatRoomTo(
        chatroom.getId().toString(),
        chatroom.getName(),
        chatroom
            .getMessages()
            .map(MessageTo::from)
            .collectList()
            .block());
  }
}
