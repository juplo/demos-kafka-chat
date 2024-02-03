package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.storage.mongodb.ChatRoomRepository;
import de.juplo.kafka.chat.backend.storage.mongodb.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@TestPropertySource(properties = {
    "chat.backend.inmemory.sharding-strategy=none",
    "chat.backend.inmemory.storage-strategy=mongodb" })
@Testcontainers
@EnableAutoConfiguration
@Slf4j
public class InMemoryWithMongoDbStorageIT extends AbstractInMemoryStorageIT
{
  @Container
  @ServiceConnection
  private static final GenericContainer MONGODB = new MongoDBContainer("mongo:6");

  @BeforeEach
  void resetStorage(
      @Autowired ChatRoomRepository chatRoomRepository,
      @Autowired MessageRepository messageRepository)
  {
    chatRoomRepository.deleteAll().block();
    messageRepository.deleteAll().block();
  }

  @BeforeEach
  void setUpLogging()
  {
    Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
    MONGODB.followOutput(logConsumer);
  }
}
