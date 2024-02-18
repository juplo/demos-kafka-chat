package de.juplo.kafka.chat.backend.implementation.kafka;

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
  private final InfoChannel infoChannel;
  private final DataChannel dataChannel;



  @Override
  public Mono<ChatRoomInfo> createChatRoom(UUID id, String name)
  {
    int shard = selectShard(id);
    log.info(
        "Sending create-command for chat rooom: id={}, name={}, shard={}",
        id,
        name,
        shard);
    return infoChannel.sendChatRoomCreatedEvent(id, name, shard);
  }

  @Override
  public Mono<ChatRoomInfo> getChatRoomInfo(UUID id)
  {
    return infoChannel
        .getChatRoomInfo(id)
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(id)));
  }

  @Override
  public Flux<ChatRoomInfo> getChatRoomInfo()
  {
    return infoChannel.getChatRoomInfo();
  }

  @Override
  public Mono<ChatRoomData> getChatRoomData(UUID id)
  {
    int shard = selectShard(id);
    return dataChannel
        .getChatRoomData(shard, id)
        .switchIfEmpty(Mono.error(() -> new UnknownChatroomException(
            id,
            shard,
            dataChannel.getOwnedShards())));
  }

  @Override
  public Mono<String[]> getShardOwners()
  {
    return infoChannel.getShardOwners();
  }

  private int selectShard(UUID chatRoomId)
  {
    byte[] serializedKey = chatRoomId.toString().getBytes();
    return Utils.toPositive(Utils.murmur2(serializedKey)) % numPartitions;
  }

  @Override
  public String toString()
  {
    StringBuffer stringBuffer = new StringBuffer(KafkaChatHomeService.class.getSimpleName());
    stringBuffer.append(", ");
    stringBuffer.append(dataChannel.getConsumerGroupMetadata());
    return stringBuffer.toString();
  }
}
