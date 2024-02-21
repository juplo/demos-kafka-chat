package de.juplo.kafka.chat.backend.storage.mongodb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Example;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static pl.rzrz.assertj.reactor.Assertions.assertThat;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.data.mongodb.host=localhost",
        "spring.data.mongodb.database=test",
        "chat.backend.inmemory.storage-strategy=mongodb" })
@Testcontainers
public class ChatRoomRepositoryIT
{
  @Container
  @ServiceConnection
  static MongoDBContainer MONGODB = new MongoDBContainer("mongo:6");

  @Autowired
  ChatRoomRepository repository;

  ChatRoomTo a, b, c;

  @BeforeEach
  public void setUp()
  {
    repository.deleteAll();

    a = repository.save(new ChatRoomTo("a", "foo"));
    b = repository.save(new ChatRoomTo("b", "bar"));
    c = repository.save(new ChatRoomTo("c", "bar"));
  }

  @Test
  public void findsAll()
  {
    assertThat(repository.findAll()).containsExactly(a, b, c);
  }

  @Test
  public void findsById()
  {
    assertThat(repository.findById("a")).contains(a);
    assertThat(repository.findById("b")).contains(b);
    assertThat(repository.findById("c")).contains(c);
    assertThat(repository.findById("666")).isEmpty();
  }

  @Test
  public void findsByExample()
  {
    assertThat(repository.findAll(Example.of(new ChatRoomTo(null, "foo")))).containsExactly(a);
    assertThat(repository.findAll(Example.of(new ChatRoomTo(null, "bar")))).containsExactly(b, c);
    assertThat(repository.findAll(Example.of(new ChatRoomTo(null, "foobar")))).isEmpty();
  }
}
