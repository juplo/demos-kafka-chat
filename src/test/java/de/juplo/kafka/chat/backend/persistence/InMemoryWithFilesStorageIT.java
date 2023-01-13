package de.juplo.kafka.chat.backend.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.api.ShardingStrategy;
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
import java.util.function.Supplier;


@Slf4j
public class InMemoryWithFilesStorageIT extends AbstractStorageStrategyIT
{
  final static Path path = Paths.get("target","local-json-files");

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
        messageFlux -> new InMemoryChatRoomService(messageFlux),
        mapper);
  }


  @Override
  protected StorageStrategy getStorageStrategy()
  {
    return storageStrategy;
  }

  @Override
  protected Supplier<ChatHomeService> getChatHomeServiceSupplier()
  {
    return () -> new InMemoryChatHomeService(1, getStorageStrategy().read());
  }

  @Override
  protected ChatRoomFactory getChatRoomFactory()
  {
    ShardingStrategy strategy = chatRoomId -> 0;
    return new InMemoryChatRoomFactory(strategy, clock, 8);
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
