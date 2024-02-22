package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import de.juplo.kafka.chat.backend.storage.mongodb.ChatRoomRepository;
import de.juplo.kafka.chat.backend.storage.mongodb.MessageRepository;
import de.juplo.kafka.chat.backend.storage.mongodb.MongoDbStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;


@Testcontainers
@ExtendWith({SpringExtension.class})
@EnableAutoConfiguration
@AutoConfigureDataMongo
@Slf4j
public class InMemoryWithMongoDbStorageIT extends AbstractInMemoryStorageIT
{
  @Autowired
  MongoDbStorageStrategy storageStrategy;
  @Autowired
  ChatRoomRepository chatRoomRepository;
  @Autowired
  MessageRepository messageRepository;


  public InMemoryWithMongoDbStorageIT()
  {
    super(Clock.systemDefaultZone());
  }


  @Override
  protected StorageStrategy getStorageStrategy()
  {
    return storageStrategy;
  }

  @TestConfiguration
  static class InMemoryWithMongoDbStorageStrategyITConfig
  {
    @Bean
    MongoDbStorageStrategy storageStrategy(
        ChatRoomRepository chatRoomRepository,
        MessageRepository messageRepository)
    {
      return new MongoDbStorageStrategy(chatRoomRepository, messageRepository);
    }

    @Bean
    Clock clock()
    {
      return Clock.systemDefaultZone();
    }
  }

  @Container
  @ServiceConnection
  private static final GenericContainer MONGODB = new MongoDBContainer("mongo:6");

  @BeforeEach
  void setUpLogging()
  {
    Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
    MONGODB.followOutput(logConsumer);
    chatRoomRepository.deleteAll();
    messageRepository.deleteAll();
  }
}
