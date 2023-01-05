package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.Chatroom;
import de.juplo.kafka.chat.backend.domain.Message;
import reactor.core.publisher.Flux;


public interface StorageStrategy
{
  void writeChatrooms(Flux<Chatroom> chatroomFlux);
  Flux<Chatroom> readChatrooms();
  void writeMessages(ChatroomInfo chatroomInfo, Flux<Message> messageFlux);
  Flux<Message> readMessages(ChatroomInfo chatroomInfo);
}
