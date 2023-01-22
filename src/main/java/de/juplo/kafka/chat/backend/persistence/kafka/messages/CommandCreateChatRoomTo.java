package de.juplo.kafka.chat.backend.persistence.kafka.messages;

import lombok.*;


@Getter
@Setter
@EqualsAndHashCode
@ToString
public class CommandCreateChatRoomTo extends AbstractMessageTo
{
  private String name;


  public CommandCreateChatRoomTo()
  {
    super(ToType.COMMAND_CREATE_CHATROOM);
  }


  public static CommandCreateChatRoomTo of(String name)
  {
    CommandCreateChatRoomTo to = new CommandCreateChatRoomTo();
    to.name = name;
    return to;
  }
}
