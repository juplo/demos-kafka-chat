package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatHomeService;
import de.juplo.kafka.chat.backend.persistence.InMemoryWithMongoDbStorageStrategyIT.DataSourceInitializer;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import de.juplo.kafka.chat.backend.persistence.storage.mongodb.ChatRoomRepository;
import de.juplo.kafka.chat.backend.persistence.storage.mongodb.MongoDbStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.util.function.Supplier;


@Testcontainers
@ExtendWith({SpringExtension.class})
@EnableAutoConfiguration
@AutoConfigureDataMongo
@ContextConfiguration(initializers = DataSourceInitializer.class)
@Slf4j
public class InMemoryWithMongoDbStorageStrategyIT extends AbstractStorageStrategyIT
{
  @Autowired
  MongoDbStorageStrategy storageStrategy;
  @Autowired
  Clock clock;


  @Override
  protected StorageStrategy getStorageStrategy()
  {
    return storageStrategy;
  }

  @Override
  protected Supplier<ChatHomeService> getChatHomeServiceSupplier()
  {
    return () -> new InMemoryChatHomeService(getStorageStrategy().read(), clock, 8);
  }


  @TestConfiguration
  static class InMemoryWithMongoDbStorageStrategyITConfig
  {
    @Bean
    MongoDbStorageStrategy storageStrategy(
        ChatRoomRepository chatRoomRepository,
        Clock clock)
    {
      return new MongoDbStorageStrategy(
          chatRoomRepository,
          clock,
          8,
          messageFlux -> new InMemoryChatRoomService(messageFlux));
    }

    @Bean
    Clock clock()
    {
      return Clock.systemDefaultZone();
    }
  }

  private static final int MONGODB_PORT = 27017;

  @Container
  private static final GenericContainer CONTAINER =
      new GenericContainer("mongo:6")
          .withExposedPorts(MONGODB_PORT);

  public static class DataSourceInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext>
  {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext)
    {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
          applicationContext,
          "spring.data.mongodb.host=localhost",
          "spring.data.mongodb.port=" + CONTAINER.getMappedPort(MONGODB_PORT),
          "spring.data.mongodb.database=test");
    }
  }

  @BeforeEach
  void setUpLogging()
  {
    Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
    CONTAINER.followOutput(logConsumer);
  }
}
