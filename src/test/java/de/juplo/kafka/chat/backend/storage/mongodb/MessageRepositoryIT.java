package de.juplo.kafka.chat.backend.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Example;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static pl.rzrz.assertj.reactor.Assertions.assertThat;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.data.mongodb.host=localhost",
        "spring.data.mongodb.database=test",
        "chat.backend.inmemory.storage-strategy=mongodb" })
@Testcontainers
public class MessageRepositoryIT
{
  @Container
  @ServiceConnection
  static MongoDBContainer MONGODB = new MongoDBContainer("mongo:6");

  @Autowired
  MessageRepository repository;

  UUID foo, bar;
  MessageTo foo_1, foo_2, bar_1, bar_2, bar_3;

  @BeforeEach
  public void setUp()
  {
    repository.deleteAll().block();

    foo = UUID.randomUUID();
    bar = UUID.randomUUID();

    long serial = 0;
    LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());

    foo_1 = repository.save(MessageTo.from(
        foo,
        new Message(
            Message.MessageKey.of("peter", 1l),
            serial++,
            now.plusSeconds(serial),
            "Nachricht #" + serial))).block();
    foo_2 = repository.save(MessageTo.from(
        foo,
        new Message(
            Message.MessageKey.of("ute", 2l),
            serial++,
            now.plusSeconds(serial),
            "Nachricht #" + serial))).block();
    bar_1 = repository.save(MessageTo.from(
        bar,
        new Message(
            Message.MessageKey.of("klaus", 1l),
            serial++,
            now.plusSeconds(serial),
            "Nachricht #" + serial))).block();
    bar_2 = repository.save(MessageTo.from(
        bar,
        new Message(
            Message.MessageKey.of("beate", 2l),
            serial++,
            now.plusSeconds(serial),
            "Nachricht #" + serial))).block();
    bar_3 = repository.save(MessageTo.from(
        bar,
        new Message(
            Message.MessageKey.of("peter", 3l),
            serial++,
            now.plusSeconds(serial),
            "Nachricht #" + serial))).block();
  }

  @Test
  public void findsAll()
  {
    assertThat(repository.findAll()).emitsExactly(foo_1, foo_2, bar_1, bar_2, bar_3);
  }

  @Test
  public void findsByExample_chatRoomId()
  {
    assertThat(repository.findAll(Example.of(new MessageTo(foo.toString(), null, null, null, null, null)))).emitsExactly(foo_1, foo_2);
    assertThat(repository.findAll(Example.of(new MessageTo(bar.toString(), null, null, null, null, null)))).emitsExactly(bar_1, bar_2, bar_3);
  }

  @Test
  public void findsByExample_user()
  {
    assertThat(repository.findAll(Example.of(new MessageTo(null, "peter", null, null, null, null)))).emitsExactly(foo_1, bar_3);
    assertThat(repository.findAll(Example.of(new MessageTo(null, "klaus", null, null, null, null)))).emitsExactly(bar_1);
    assertThat(repository.findAll(Example.of(new MessageTo(null, "ute",   null, null, null, null)))).emitsExactly(foo_2);
    assertThat(repository.findAll(Example.of(new MessageTo(null, "beate", null, null, null, null)))).emitsExactly(bar_2);
  }

  @Test
  public void findsByExample_id()
  {
    assertThat(repository.findAll(Example.of(new MessageTo(null, null, 1l, null, null, null)))).emitsExactly(foo_1, bar_1);
    assertThat(repository.findAll(Example.of(new MessageTo(null, null, 2l, null, null, null)))).emitsExactly(foo_2, bar_2);
    assertThat(repository.findAll(Example.of(new MessageTo(null, null, 3l, null, null, null)))).emitsExactly(bar_3);
  }
}
