package de.juplo.kafka.chat.backend.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.implementation.kafka.ChannelNotReadyException;
import de.juplo.kafka.chat.backend.domain.exceptions.UnknownChatroomException;
import de.juplo.kafka.chat.backend.implementation.inmemory.InMemoryServicesConfiguration;
import de.juplo.kafka.chat.backend.implementation.kafka.KafkaServicesConfiguration;
import de.juplo.kafka.chat.backend.storage.files.FilesStorageConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static pl.rzrz.assertj.reactor.Assertions.assertThat;


@SpringJUnitConfig(classes = {
    InMemoryServicesConfiguration.class,
    FilesStorageConfiguration.class,
    KafkaServicesConfiguration.class,
    AbstractChatHomeServiceIT.TestConfiguration.class })
@EnableConfigurationProperties(ChatBackendProperties.class)
public abstract class AbstractChatHomeServiceIT
{
  @Autowired
  ChatHomeService chatHomeService;


  @Test
  @DisplayName("Assert chatroom is delivered, if it exists")
  void testGetExistingChatroom()
  {
    // Given
    UUID chatRoomId = UUID.fromString("5c73531c-6fc4-426c-adcb-afc5c140a0f7");

    // When
    Mono<ChatRoomData> mono = Mono
        .defer(() -> chatHomeService.getChatRoomData(chatRoomId))
        .log("testGetExistingChatroom")
        .retryWhen(Retry
            .backoff(5, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof ChannelNotReadyException));

    // Then
    assertThat(mono).emitsCount(1);
  }

  @Test
  @DisplayName("Assert UnknownChatroomException is thrown, if chatroom does not exist")
  void testGetNonExistentChatroom()
  {
    // Given
    UUID chatRoomId = UUID.fromString("7f59ec77-832e-4a17-8d22-55ef46242c17");

    // When
    Mono<ChatRoomData> mono = Mono
        .defer(() -> chatHomeService.getChatRoomData(chatRoomId))
        .log("testGetNonExistentChatroom")
        .retryWhen(Retry
            .backoff(5, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof ChannelNotReadyException));

    // Then
    assertThat(mono).sendsError(e ->
    {
      assertThat(e).isInstanceOf(UnknownChatroomException.class);
      UnknownChatroomException unknownChatroomException = (UnknownChatroomException) e;
      assertThat(unknownChatroomException.getChatroomId()).isEqualTo(chatRoomId);
    });
  }

  static class TestConfiguration
  {
    @Bean
    ObjectMapper objectMapper()
    {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper;
    }

    @Bean
    Clock clock()
    {
      return Clock.systemDefaultZone();
    }
  }
}
