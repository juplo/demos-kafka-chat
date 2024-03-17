package de.juplo.kafka.chat.backend.implementation.haproxy;

import de.juplo.kafka.chat.backend.domain.ShardingPublisherStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


@RequiredArgsConstructor
@Slf4j
public class HaproxyRuntimeApiShardingPublisherStrategy implements ShardingPublisherStrategy
{
  private final SocketAddress haproxyAddress;
  private final String map;
  private final String instanceId;


  @Override
  public Mono<String> publishOwnership(int shard)
  {
    try
    {
      SocketChannel socketChannel = SocketChannel.open(haproxyAddress);
      String command = "set map " + map + " " + Integer.toString(shard) + " " + instanceId + "\n";
      byte[] commandBytes = command.getBytes();
      ByteBuffer buffer = ByteBuffer.wrap(commandBytes);
      socketChannel.write(buffer);
      socketChannel.close();
      return Mono.just(instanceId);
    }
    catch (Exception e)
    {
      return Mono.error(e);
    }
  }
}
