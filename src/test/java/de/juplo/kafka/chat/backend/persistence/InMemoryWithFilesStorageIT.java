package de.juplo.kafka.chat.backend.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoomFactory;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomFactory;
import de.juplo.kafka.chat.backend.persistence.storage.files.FilesStorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatHomeService;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;


@Slf4j
public class InMemoryWithFilesStorageIT extends AbstractStorageStrategyIT
{
  final static Path path = Paths.get("target","files");

  final Clock clock;
  final ObjectMapper mapper;
  final FilesStorageStrategy storageStrategy;


  public InMemoryWithFilesStorageIT()
  {
    clock = Clock.systemDefaultZone();
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    storageStrategy = new FilesStorageStrategy(
        path,
        clock,
        8,
        chatRoomId -> 0,
        messageFlux -> new InMemoryChatRoomService(messageFlux),
        mapper);
  }


  @Override
  protected StorageStrategy getStorageStrategy()
  {
    return storageStrategy;
  }

  @Override
  protected StorageStrategyITConfig getConfig()
  {
    return new StorageStrategyITConfig()
    {
      InMemoryChatHomeService chatHomeService = new InMemoryChatHomeService(
          1,
          new int[] { 0 },
          getStorageStrategy().read());

      InMemoryChatRoomFactory chatRoomFactory = new InMemoryChatRoomFactory(
          chatRoomId -> 0,
          clock,
          8);

      @Override
      public ChatHomeService getChatHomeService()
      {
        return chatHomeService;
      }

      @Override
      public ChatRoomFactory getChatRoomFactory()
      {
        return chatRoomFactory;
      }
    };
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
