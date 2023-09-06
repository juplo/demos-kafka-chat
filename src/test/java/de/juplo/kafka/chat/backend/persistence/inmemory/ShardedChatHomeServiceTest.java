package de.juplo.kafka.chat.backend.persistence.inmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.domain.ChatHomeServiceWithShardsTest;
import de.juplo.kafka.chat.backend.persistence.ShardingStrategy;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.storage.files.FilesStorageStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.file.Paths;
import java.time.Clock;
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

      return new ShardedChatHomeService(chatHomes, strategy);
    }

    @Bean
    public FilesStorageStrategy storageStrategy(Clock clock)
    {
      return new FilesStorageStrategy(
          Paths.get("target", "test-classes", "data", "files"),
          new KafkaLikeShardingStrategy(NUM_SHARDS),
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
