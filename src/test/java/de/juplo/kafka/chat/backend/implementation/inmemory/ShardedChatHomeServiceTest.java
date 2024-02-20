package de.juplo.kafka.chat.backend.implementation.inmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.domain.ChatHomeServiceWithShardsTest;
import de.juplo.kafka.chat.backend.implementation.ShardingStrategy;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import de.juplo.kafka.chat.backend.storage.files.FilesStorageStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.logging.Level;
import java.util.stream.IntStream;

public class ShardedChatHomeServiceTest extends ChatHomeServiceWithShardsTest
{
  @TestConfiguration
  static class Configuration
  {
    @Bean
    ShardedChatHomeService chatHome(
        StorageStrategy storageStrategy,
        Clock clock)
    {
      SimpleChatHomeService[] chatHomes = new SimpleChatHomeService[NUM_SHARDS];

      IntStream
          .of(ownedShards())
          .forEach(shard -> chatHomes[shard] = new SimpleChatHomeService(
              shard,
              storageStrategy,
              clock,
              bufferSize()));

      ShardingStrategy strategy = new KafkaLikeShardingStrategy(NUM_SHARDS);

      return new ShardedChatHomeService(
          "http://instance-0",
          chatHomes,
          IntStream
              .range(0, NUM_SHARDS)
              .mapToObj(shard -> "http://instance-0")
              .map(uriString -> URI.create(uriString))
              .toArray(size -> new URI[size]),
          strategy);
    }

    @Bean
    FilesStorageStrategy storageStrategy(
        Clock clock,
        ObjectMapper objectMapper)
    {
      return new FilesStorageStrategy(
          Paths.get("target", "test-classes", "data", "files"),
          new KafkaLikeShardingStrategy(NUM_SHARDS),
          objectMapper,
          Level.FINE,
          true);
    }

    @Bean
    ObjectMapper objectMapper()
    {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper;
    }

    @Bean
    Clock clock()
    {
      return Clock.systemDefaultZone();
    }

    int[] ownedShards()
    {
      return new int[] { OWNED_SHARD };
    }

    int bufferSize()
    {
      return 8;
    }
  }
}
