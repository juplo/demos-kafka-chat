package de.juplo.kafka.chat.backend.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


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

  public static ChatRoomTo from(ChatRoomInfo chatRoomInfo)
  {
    return new ChatRoomTo(
        chatRoomInfo.getId().toString(),
        chatRoomInfo.getName());
  }
}
