package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@TestPropertySource(properties = {
    "chat.backend.inmemory.sharding-strategy=none",
    "chat.backend.inmemory.storage-strategy=files",
    "chat.backend.inmemory.storage-directory=target/files" })
@Slf4j
public class InMemoryWithFilesStorageIT extends AbstractInMemoryStorageIT
{
  @BeforeEach
  void resetStorage(
      @Autowired ChatBackendProperties properties)
      throws Exception
  {
    Path path = Paths.get(properties.getInmemory().getStorageDirectory());
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
