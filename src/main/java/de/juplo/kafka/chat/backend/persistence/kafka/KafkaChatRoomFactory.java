package de.juplo.kafka.chat.backend.persistence.kafka;

import de.juplo.kafka.chat.backend.domain.ChatRoomFactory;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RequiredArgsConstructor
@Slf4j
public class KafkaChatRoomFactory implements ChatRoomFactory
{
  private final ChatRoomChannel chatRoomChannel;

  @Override
  public Mono<ChatRoomInfo> createChatRoom(UUID id, String name)
  {
    log.info("Sending create-command for chat rooom: id={}, name={}");
    return chatRoomChannel.sendCreateChatRoomRequest(id, name);
  }
}
