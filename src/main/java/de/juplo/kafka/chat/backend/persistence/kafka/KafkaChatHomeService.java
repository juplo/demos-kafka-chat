package de.juplo.kafka.chat.backend.persistence.kafka;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomData;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.exceptions.UnknownChatroomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.utils.Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@RequiredArgsConstructor
@Slf4j
public class KafkaChatHomeService implements ChatHomeService
{
  private final int numPartitions;
  private final ChatRoomChannel chatRoomChannel;



  @Override
  public Mono<ChatRoomInfo> createChatRoom(UUID id, String name)
  {
    log.info("Sending create-command for chat rooom: id={}, name={}");
    return chatRoomChannel.sendCreateChatRoomRequest(id, name);
  }

  @Override
  public Mono<ChatRoomInfo> getChatRoomInfo(UUID id)
  {
    int shard = selectShard(id);
    return chatRoomChannel
        .getChatRoomInfo(shard, id)
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(
            id,
            shard,
            chatRoomChannel.getOwnedShards())));
  }

  @Override
  public Flux<ChatRoomInfo> getChatRoomInfo()
  {
    return chatRoomChannel.getChatRoomInfo();
  }

  @Override
  public Mono<ChatRoomData> getChatRoomData(UUID id)
  {
    int shard = selectShard(id);
    return chatRoomChannel
        .getChatRoomData(shard, id)
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(
            id,
            shard,
            chatRoomChannel.getOwnedShards())));
  }

  public Flux<ChatRoomData> getChatRoomData()
  {
      return chatRoomChannel.getChatRoomData();
  }

  int selectShard(UUID chatRoomId)
  {
    byte[] serializedKey = chatRoomId.toString().getBytes();
    return Utils.toPositive(Utils.murmur2(serializedKey)) % numPartitions;
  }
}
