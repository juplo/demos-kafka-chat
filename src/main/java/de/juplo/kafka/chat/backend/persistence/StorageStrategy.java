package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.api.ChatroomTo;
import de.juplo.kafka.chat.backend.domain.Chatroom;
import de.juplo.kafka.chat.backend.domain.Message;
import reactor.core.publisher.Flux;


public interface StorageStrategy
{
  void writeChatrooms(Flux<Chatroom> chatroomFlux);
  Flux<Chatroom> readChatrooms();
  void writeMessages(ChatroomTo chatroomTo, Flux<Message> messageFlux);
  Flux<Message> readMessages(ChatroomTo chatroomTo);
}