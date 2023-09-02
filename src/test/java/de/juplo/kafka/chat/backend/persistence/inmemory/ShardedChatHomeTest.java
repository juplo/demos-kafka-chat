package de.juplo.kafka.chat.backend.persistence.inmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.domain.ChatHomeWithShardsTest;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.storage.files.FilesStorageStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.file.Paths;
import java.time.Clock;
import java.util.stream.IntStream;

public class ShardedChatHomeTest extends ChatHomeWithShardsTest
{
  @TestConfiguration
  static class Configuration
  {
    @Bean
    ShardedChatHome chatHome(
        StorageStrategy storageStrategy,
        Clock clock)
    {
      SimpleChatHome[] chatHomes = new SimpleChatHome[NUM_SHARDS];

      IntStream
          .of(ownedShards())
          .forEach(shard -> chatHomes[shard] = new SimpleChatHome(
              shard,
              storageStrategy.read(),
              clock,
              bufferSize()));

      ShardingStrategy strategy = new KafkaLikeShardingStrategy(NUM_SHARDS);

      return new ShardedChatHome(chatHomes, strategy);
    }

    @Bean
    public FilesStorageStrategy storageStrategy(Clock clock)
    {
      return new FilesStorageStrategy(
          Paths.get("target", "test-classes", "data", "files"),
          clock,
          bufferSize(),
          new KafkaLikeShardingStrategy(NUM_SHARDS),
          messageFlux -> new InMemoryChatRoomService(messageFlux),
          new ObjectMapper());
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
