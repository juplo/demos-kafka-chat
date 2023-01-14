package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.ShardingStrategy;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatRoomFactory;
import de.juplo.kafka.chat.backend.domain.ChatRoomService;
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
  private final ShardingStrategy shardingStrategy;
  private final Clock clock;
  private final int bufferSize;


  @Override
  public Mono<ChatRoom> createChatRoom(UUID id, String name)
  {
    log.info("Creating ChatRoom with buffer-size {}", bufferSize);
    ChatRoomService service = new InMemoryChatRoomService(Flux.empty());
    return Mono.just(new ChatRoom(id, name, clock, service, bufferSize));
  }
}
