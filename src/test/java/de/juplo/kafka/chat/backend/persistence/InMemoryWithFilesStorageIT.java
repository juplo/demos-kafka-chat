package de.juplo.kafka.chat.backend.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.persistence.storage.files.FilesStorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.InMemoryChatRoomService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;


@Slf4j
public class InMemoryWithFilesStorageIT extends AbstractInMemoryStorageIT
{
  final static Path path = Paths.get("target","files");

  final ObjectMapper mapper;
  final FilesStorageStrategy storageStrategy;


  public InMemoryWithFilesStorageIT()
  {
    super(Clock.systemDefaultZone());
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
