package de.juplo.kafka.chat.backend.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.UUID;


@RequiredArgsConstructor
@Slf4j
public class MongoDbStorageStrategy implements StorageStrategy
{
  private final ChatRoomRepository chatRoomRepository;
  private final MessageRepository messageRepository;


  @Override
  public Flux<ChatRoomInfo> writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux)
  {
    return chatRoomInfoFlux
        .map(ChatRoomTo::from)
        .map(chatRoomRepository::save)
        .map(ChatRoomTo::toChatRoomInfo);
  }

  @Override
  public Flux<ChatRoomInfo> readChatRoomInfo()
  {
    return Flux
        .fromIterable(chatRoomRepository.findAll())
        .map(ChatRoomTo::toChatRoomInfo);
  }

  @Override
  public Flux<Message> writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux)
  {
    return messageFlux
        .map(message -> MessageTo.from(chatRoomId, message))
        .map(messageRepository::save)
        .map(MessageTo::toMessage);
  }

  @Override
  public Flux<Message> readChatRoomData(UUID chatRoomId)
  {
    return Flux
        .fromIterable(messageRepository.findByChatRoomIdOrderBySerialAsc(chatRoomId.toString()))
        .map(MessageTo::toMessage);
  }
}
