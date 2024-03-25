package de.juplo.kafka.chat.backend.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;


@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Slf4j
public abstract class AbstractConfigurationIT
{
  final static String EXISTING_CHATROOM = "5c73531c-6fc4-426c-adcb-afc5c140a0f7";
  String NONEXISTENT_CHATROOM = "7f59ec77-832e-4a17-8d22-55ef46242c17";


  @Autowired
  WebTestClient webTestClient;
  @Autowired
  ObjectMapper objectMapper;

  @Value("classpath:data/files/5c73531c-6fc4-426c-adcb-afc5c140a0f7.json")
  Resource existingChatRoomRessource;
  MessageTo[] expectedExistingMessages;


  @BeforeEach
  void waitForApp() throws IOException
  {
    expectedExistingMessages = objectMapper
        .readValue(
            existingChatRoomRessource.getInputStream(),
            new TypeReference<List<MessageTo>>() {})
        .toArray(size -> new MessageTo[size]);

    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri("/actuator/health")
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.status").isEqualTo("UP");
        });
  }

  @Test
  @DisplayName("Restored chat-rooms can be listed")
  void testRestoredChatRoomsCanBeListed()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          AtomicBoolean existingChatRoomFound = new AtomicBoolean(false);
          webTestClient
              .get()
              .uri("/list")
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .returnResult(ChatRoomInfoTo.class)
              .getResponseBody()
              .toIterable()
              .forEach(chatRoomInfoTo ->
              {
                log.debug("Inspecting chat-room {}", chatRoomInfoTo);
                if (chatRoomInfoTo.getId().equals(UUID.fromString(EXISTING_CHATROOM)))
                {
                  log.debug("Found existing chat-room {}", chatRoomInfoTo);
                  existingChatRoomFound.set(true);
                  assertThat(chatRoomInfoTo.getName().equals("FOO"));
                }
              });
          assertThat(existingChatRoomFound.get()).isTrue();
        });
  }

  @Test
  @DisplayName("Details as expected for restored chat-room")
  void testRestoredChatRoomHasExpectedDetails()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri("/{chatRoomId}", EXISTING_CHATROOM)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.name").isEqualTo("FOO");
        });
  }

  @Test
  @DisplayName("Restored message from Ute has expected Text")
  void testRestoredMessageForUteHasExpectedText()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri("/{chatRoomId}/ute/1", EXISTING_CHATROOM)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.text").isEqualTo("Ich bin Ute...");
        });
  }

  @Test
  @DisplayName("Restored message from Peter has expected Text")
  void testRestoredMessageForPeterHasExpectedText()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .get()
              .uri("/{chatRoomId}/peter/1", EXISTING_CHATROOM)
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.text").isEqualTo("Hallo, ich heiße Peter!");
        });
  }

  @Test
  @DisplayName("A PUT-message for a non-existent chat-room yields 404 NOT FOUND")
  void testNotFoundForPutMessageToNonExistentChatRoom()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .put()
              .uri("/{chatRoomId}/otto/66", NONEXISTENT_CHATROOM)
              .contentType(MediaType.TEXT_PLAIN)
              .accept(MediaType.APPLICATION_JSON)
              .bodyValue("The devil rules route 66")
              .exchange()
              .expectStatus().isNotFound()
              .expectBody()
                .jsonPath("$.type").value(endsWith("/problem/unknown-chatroom"))
                .jsonPath("$.chatroomId").isEqualTo(NONEXISTENT_CHATROOM);
        });
  }

  @Test
  @DisplayName("A message can be put into a newly created chat-room")
  void testPutMessageInNewChatRoom() throws IOException
  {
    ChatRoomInfoTo chatRoomInfo;
    do
    {
      // The first request creates a new chat-room
      // It must be repeated, until a chat-room was created,
      // that is owned by the instance
      chatRoomInfo = webTestClient
          .post()
          .uri("/create")
          .contentType(MediaType.TEXT_PLAIN)
          .bodyValue("bar")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .returnResult(ChatRoomInfoTo.class)
          .getResponseBody()
          .retry(30)
          .blockFirst();
    }
    while(!(chatRoomInfo.getShard() == null || chatRoomInfo.getShard().intValue() == 2));

    UUID chatRoomId = chatRoomInfo.getId();

    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() ->
        {
          webTestClient
              .put()
              .uri("/{chatRoomId}/nerd/7", chatRoomId)
              .contentType(MediaType.TEXT_PLAIN)
              .accept(MediaType.APPLICATION_JSON)
              .bodyValue("Hello world!")
              .exchange()
              .expectStatus().isOk()
              .expectBody()
                .jsonPath("$.id").isEqualTo(Integer.valueOf(7))
                .jsonPath("$.user").isEqualTo("nerd")
                .jsonPath("$.text").isEqualTo("Hello world!");
        });
  }

  @Test
  @DisplayName("Only newly send messages can be seen, when listening to restored chat-room")
  void testListenToRestoredChatRoomYieldsOnlyNewlyAddedMessages()
  {
    MessageTo sentMessage = webTestClient
        .put()
        .uri(
            "/{chatRoomId}/nerd/{messageId}",
            EXISTING_CHATROOM,
            RandomGenerator.getDefault().nextInt())
        .contentType(MediaType.TEXT_PLAIN)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue("Hello world!")
        .exchange()
        .expectStatus()
        .isOk()
        .returnResult(MessageTo.class)
        .getResponseBody()
        .next()
        .block();

    Flux<MessageTo> result = webTestClient
        .get()
        .uri("/{chatRoomId}/listen", EXISTING_CHATROOM)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isOk()
        .returnResult(MessageTo.class)
        .getResponseBody();

    assertThat(result.next().block()).isEqualTo(sentMessage);
  }
}
