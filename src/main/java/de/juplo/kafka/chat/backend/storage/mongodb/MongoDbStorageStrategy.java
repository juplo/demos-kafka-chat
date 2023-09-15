package de.juplo.kafka.chat.backend.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.implementation.ShardingStrategy;
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
  private final ShardingStrategy shardingStrategy;


  @Override
  public void writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux)
  {
    chatRoomInfoFlux
        .map(ChatRoomTo::from)
        .subscribe(chatroomTo -> chatRoomRepository.save(chatroomTo));
  }

  @Override
  public Flux<ChatRoomInfo> readChatRoomInfo()
  {
    return Flux
        .fromIterable(chatRoomRepository.findAll())
        .map(chatRoomTo ->
        {
          UUID chatRoomId = UUID.fromString(chatRoomTo.getId());
          return new ChatRoomInfo(chatRoomId, chatRoomTo.getName(), null);
        });
  }

  @Override
  public void writeChatRoomData(UUID chatRoomId, Flux<Message> messageFlux)
  {
    messageFlux
        .map(message -> MessageTo.from(chatRoomId, message))
        .subscribe(messageTo -> messageRepository.save(messageTo));
  }

  @Override
  public Flux<Message> readChatRoomData(UUID chatRoomId)
  {
    return Flux
        .fromIterable(messageRepository.findByChatRoomIdOrderBySerialAsc(chatRoomId.toString()))
        .map(messageTo -> messageTo.toMessage());
  }
}
