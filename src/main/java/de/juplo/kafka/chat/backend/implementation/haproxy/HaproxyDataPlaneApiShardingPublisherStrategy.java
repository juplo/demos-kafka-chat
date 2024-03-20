package de.juplo.kafka.chat.backend.implementation.haproxy;

import de.juplo.kafka.chat.backend.domain.ShardingPublisherStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;


@RequiredArgsConstructor
@Slf4j
public class HaproxyDataPlaneApiShardingPublisherStrategy implements ShardingPublisherStrategy
{
  public final static String MAP_ENTRY_PATH = "/services/haproxy/runtime/maps_entries/{key}";
  public final static String MAP_PARAM = "map";
  public final static String FORCE_SYNC_PARAM = "force_sync";


  private final WebClient webClient;
  private final String mapName;
  private final String instanceId;


  @Override
  public Mono<String> publishOwnership(int shard)
  {
    return webClient
        .put()
        .uri(uriBuilder -> uriBuilder
            .path(MAP_ENTRY_PATH)
            .queryParam(MAP_PARAM, mapName)
            .queryParam(FORCE_SYNC_PARAM, 1)
            .build(Map.of("key", shard)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new MapEntryTo(Integer.toString(shard), instanceId))
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToMono(response ->
        {
          HttpStatusCode statusCode = response.statusCode();
          if (statusCode.equals(HttpStatus.OK))
          {
            return response.bodyToMono(MapEntryTo.class);
          }
          else
          {
            return response
                .<MapEntryTo>createError()
                .onErrorMap(
                    WebClientResponseException.class,
                    e -> new DataPlaneApiErrorException(
                        e.getStatusCode(),
                        e.getResponseBodyAs(DataPlaneApiErrorTo.class)));
          }
        })
        .map(entry -> entry.value());
  }
}
