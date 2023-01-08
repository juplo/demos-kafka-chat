package de.juplo.kafka.chat.backend.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.Message;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@Slf4j
public class LocalJsonFilesStorageStrategyIT
{
  final static Path path = Paths.get("target","local-json-files");

  InMemoryChatHomeService service;
  StorageStrategy storageStrategy;
  ChatHome chathome;

  void start()
  {
    Clock clock = Clock.systemDefaultZone();
    service = new InMemoryChatHomeService(clock, 8);
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    storageStrategy = new LocalJsonFilesStorageStrategy(path, mapper, service);
    chathome = new ChatHome(service, storageStrategy.readChatrooms());
  }

  void stop()
  {
    storageStrategy.writeChatrooms(Flux.fromStream(chathome.list()));
  }

  @Test
  void testStoreAndRecreate()
  {
    start();

    assertThat(chathome.list()).hasSize(0);

    ChatRoom chatroom = chathome.createChatroom("FOO");
    Message m1 = chatroom.addMessage(1l,"Peter", "Hallo, ich heiße Peter!").block();
    Message m2 = chatroom.addMessage(1l, "Ute", "Ich bin Ute...").block();
    Message m3 = chatroom.addMessage(2l, "Peter", "Willst du mit mir gehen?").block();
    Message m4 = chatroom.addMessage(1l, "Klaus", "Ja? Nein? Vielleicht??").block();

    assertThat(chathome.list()).containsExactlyElementsOf(List.of(chatroom));
    assertThat(chathome.getChatroom(chatroom.getId())).contains(chatroom);
    assertThat(chathome.getChatroom(chatroom.getId()).get().getMessages().toStream()).containsExactlyElementsOf(List.of(m1, m2, m3, m4));

    stop();
    start();

    assertThat(chathome.list()).containsExactlyElementsOf(List.of(chatroom));
    assertThat(chathome.getChatroom(chatroom.getId())).contains(chatroom);
    assertThat(chathome.getChatroom(chatroom.getId()).get().getMessages().toStream()).containsExactlyElementsOf(List.of(m1, m2, m3, m4));
  }

  @BeforeEach
  void reset() throws Exception
  {
    if (Files.exists(path))
    {
      Files
          .walk(path)
          .forEach(file ->
          {
            try
            {
              if (!file.equals(path))
              {
                log.debug("Deleting file {}", file);
                Files.delete(file);
              }
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          });
      log.debug("Deleting data-directory {}", path);
      Files.delete(path);
    }
  }
}
