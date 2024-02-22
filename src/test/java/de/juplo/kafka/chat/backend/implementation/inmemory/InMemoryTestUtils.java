package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Clock;


public class InMemoryTestUtils
{
  @Autowired
  StorageStrategy storageStrategy;


  public Mono<Void> restore(SimpleChatHomeService simpleChatHomeService)
  {
    return simpleChatHomeService.restore(storageStrategy);
  }
}
