package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;


@Slf4j
public class TestClient
{
  public void run()
  {
    for (int i = 0; i < 100; i++)
    {
      String message = "Message #" + i;
      for (ChatRoomInfoTo chatRoom : chatRooms)
      {
        sendMessage(chatRoom, message)
            .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(1)))
            .map(MessageTo::toString)
            .subscribe(result -> log.info(
                "{} sent message \"{}\" to {}",
                user,
                message,
                chatRoom));
      }
    }
  }

  private Mono<MessageTo> sendMessage(
      ChatRoomInfoTo chatRoom,
      String message)
  {
    return webClient
        .put()
        .uri(
            "/{chatRoomId}/{username}/{serial}",
            chatRoom.getId(),
            user.getName(),
            user.nextSerial())
        .contentType(MediaType.TEXT_PLAIN)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(message)
        .exchangeToMono(response ->
        {
          if (response.statusCode().equals(HttpStatus.OK))
          {
            return response.bodyToMono(MessageTo.class);
          }
          else
          {
            return response.createError();
          }
        });
  }


  private final WebClient webClient;
  private final ChatRoomInfoTo[] chatRooms;
  private final User user;


  TestClient(Integer port, ChatRoomInfoTo[] chatRooms, String username)
  {
    webClient = WebClient.create("http://localhost:" + port);
    this.chatRooms = chatRooms;
    user = new User(username);
  }
}
