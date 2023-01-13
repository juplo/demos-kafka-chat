package de.juplo.kafka.chat.backend.persistence.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatHomeFactory;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class InMemoryChatHomeFactory implements ChatHomeFactory
{
  private final InMemoryChatHomeService service;


  @Override
  public ChatHome createChatHome(int shard)
  {
    return new ChatHome(service, shard);
  }
}
