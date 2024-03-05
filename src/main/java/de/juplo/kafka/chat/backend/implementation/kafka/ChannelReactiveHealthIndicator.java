package de.juplo.kafka.chat.backend.implementation.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
public class ChannelReactiveHealthIndicator extends AbstractReactiveHealthIndicator
{
  private final Channel channel;


  @Override
  protected Mono<Health> doHealthCheck(Health.Builder builder)
  {
    return Mono
        .fromSupplier(() -> channel.getChannelState())
        .map(state  -> switch(state)
            {
              case STARTING -> builder.outOfService();
              case LOAD_IN_PROGRESS -> builder.outOfService();
              case READY -> builder.up();
              case SHUTTING_DOWN -> builder.down();
            })
        .map(healthBuilder -> healthBuilder.build());
  }
}
