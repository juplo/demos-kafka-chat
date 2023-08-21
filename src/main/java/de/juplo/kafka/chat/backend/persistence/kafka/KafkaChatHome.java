package de.juplo.kafka.chat.backend.persistence.kafka;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.UnknownChatroomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.utils.Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@RequiredArgsConstructor
@Slf4j
public class KafkaChatHome implements ChatHome
{
  private final int numPartitions;
  private final ChatRoomChannel chatRoomChannel;


  @Override
  public Mono<ChatRoom> getChatRoom(UUID id)
  {
    int shard = selectShard(id);
    return chatRoomChannel
        .getChatRoom(shard, id)
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(
            id,
            shard,
            chatRoomChannel.getOwnedShards())));
  }

  int selectShard(UUID chatRoomId)
  {
    byte[] serializedKey = chatRoomId.toString().getBytes();
    return Utils.toPositive(Utils.murmur2(serializedKey)) % numPartitions;
  }

  @Override
  public Flux<ChatRoom> getChatRooms()
  {
      return chatRoomChannel.getChatRooms();
  }
}
