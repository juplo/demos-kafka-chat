package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.api.ChatRoomTo;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.Message;
import reactor.core.publisher.Flux;


public interface StorageStrategy
{
  void writeChatrooms(Flux<ChatRoom> chatroomFlux);
  Flux<ChatRoom> readChatrooms();
  void writeMessages(ChatRoomTo chatroomTo, Flux<Message> messageFlux);
  Flux<Message> readMessages(ChatRoomTo chatroomTo);
}
