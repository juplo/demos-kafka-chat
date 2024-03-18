package de.juplo.kafka.chat.backend.implementation.haproxy;

import de.juplo.kafka.chat.backend.domain.ShardingPublisherStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Slf4j
public class HaproxyDataPlaneApiShardingPublisherStrategy implements ShardingPublisherStrategy
{
  @Getter
  private final int mapId;


  public HaproxyDataPlaneApiShardingPublisherStrategy(
      WebClient webClient,
      String mapPath,
      String instanceId)
  {
    this.mapId = 0;
  }


  @Override
  public Mono<String> publishOwnership(int shard)
  {
    return Mono.empty();
  }
}
