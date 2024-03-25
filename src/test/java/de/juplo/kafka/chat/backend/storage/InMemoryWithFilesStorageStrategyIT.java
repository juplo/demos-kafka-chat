package de.juplo.kafka.chat.backend.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.ChatBackendProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@TestPropertySource(properties = {
    "chat.backend.inmemory.sharding-strategy=none",
    "chat.backend.inmemory.storage-strategy=files",
    "chat.backend.inmemory.storage-directory=target/files" })
@ContextConfiguration(classes = InMemoryWithFilesStorageStrategyIT.TestConfig.class)
@Slf4j
public class InMemoryWithFilesStorageStrategyIT extends AbstractInMemoryStorageStrategyIT
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


  static class TestConfig
  {
    @Bean
    ObjectMapper objectMapper()
    {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      return objectMapper;
    }
  }
}
