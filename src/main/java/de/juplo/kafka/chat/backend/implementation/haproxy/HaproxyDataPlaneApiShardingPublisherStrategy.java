package de.juplo.kafka.chat.backend.implementation.haproxy;

import de.juplo.kafka.chat.backend.domain.ShardingPublisherStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;


@Slf4j
public class HaproxyDataPlaneApiShardingPublisherStrategy implements ShardingPublisherStrategy
{
  public final static String MAPS_PATH = "/services/haproxy/runtime/maps";
  public final static String INCLUDE_UNMANAGED_PARAM = "include_unmanaged";
  public final static String MAP_ENTRY_PATH = "/services/haproxy/runtime/maps_entries/{key}";
  public final static String MAP_PARAM = "map";


  private final WebClient webClient;
  @Getter
  private final int mapId;
  private final String instanceId;


  public HaproxyDataPlaneApiShardingPublisherStrategy(
      WebClient webClient,
      String mapPath,
      String instanceId)
  {
    this.webClient = webClient;
    this.mapId = webClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path(MAPS_PATH)
            .queryParam(INCLUDE_UNMANAGED_PARAM, Boolean.TRUE)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToFlux(response ->
        {
          if (response.statusCode().equals(HttpStatus.OK))
          {
            return response.bodyToFlux(MapInfoTo.class);
          }
          else
          {
            return response.<MapInfoTo>createError().flux();
          }
        })
        .filter(map -> map.file().trim().equals(mapPath))
        .map(map -> map.id())
        .map(id -> Integer.valueOf(id))
        .blockFirst(Duration.ofSeconds(1));
    this.instanceId = instanceId;
  }


  @Override
  public Mono<String> publishOwnership(int shard)
  {
    return webClient
        .put()
        .uri(uriBuilder -> uriBuilder
            .path(MAP_ENTRY_PATH)
            .queryParam(MAP_PARAM, "#" + mapId)
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
