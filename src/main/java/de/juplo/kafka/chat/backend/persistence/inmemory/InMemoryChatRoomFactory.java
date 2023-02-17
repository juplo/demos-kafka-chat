package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.UUID;


@RequiredArgsConstructor
@Slf4j
public class InMemoryChatRoomFactory implements ChatRoomFactory
{
  private final InMemoryChatHomeService chatHomeService;
  private final ShardingStrategy shardingStrategy;
  private final Clock clock;
  private final int bufferSize;


  @Override
  public Mono<ChatRoomInfo> createChatRoom(UUID id, String name)
  {
    log.info("Creating ChatRoom with buffer-size {}", bufferSize);
    int shard = shardingStrategy.selectShard(id);
    ChatRoomService service = new InMemoryChatRoomService(Flux.empty());
    ChatRoom chatRoom = new ChatRoom(id, name, shard, clock, service, bufferSize);
    chatHomeService.putChatRoom(chatRoom);
    return Mono.just(chatRoom);
  }
}
