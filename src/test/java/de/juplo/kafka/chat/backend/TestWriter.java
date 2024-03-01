package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
public class TestWriter implements Runnable
{
  @Override
  public void run()
  {
    for (int i = 0; running; i++)
    {
      String message = "Message #" + i;
      try
      {
        sendMessage(chatRoom, message)
            .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(1)))
            .map(MessageTo::toString)
            .onErrorResume(throwable ->
            {
              WebClientResponseException e = (WebClientResponseException)throwable.getCause();
              return Mono.just(e.getResponseBodyAsString(Charset.defaultCharset()));
            })
            .subscribe(result -> log.info(
                "{} sent a message to {}: {}",
                user,
                chatRoom,
                result));

        Thread.sleep(ThreadLocalRandom.current().nextLong(700, 1000));
      }
      catch (Exception e)
      {
        throw new RuntimeException(e);
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
  private final ChatRoomInfoTo chatRoom;
  private final User user;

  volatile boolean running = true;


  TestWriter(Integer port, ChatRoomInfoTo chatRoom, String username)
  {
    webClient = WebClient.create("http://localhost:" + port);
    this.chatRoom = chatRoom;
    user = new User(username);
  }
}
