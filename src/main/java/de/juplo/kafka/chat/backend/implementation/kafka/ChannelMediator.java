package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RequiredArgsConstructor
public class ChannelMediator
{
  @Setter
  private InfoChannel infoChannel;


  void shardAssigned(int shard)
  {
    infoChannel.sendShardAssignedEvent(shard);
  }

  void shardRevoked(int shard)
  {
    infoChannel.sendShardRevokedEvent(shard);
  }

  Mono<ChatRoomInfo> getChatRoomInfo(UUID id)
  {
    return infoChannel.getChatRoomInfo(id);
  }
}
