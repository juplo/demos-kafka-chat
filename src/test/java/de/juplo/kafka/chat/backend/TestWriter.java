package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
public class TestWriter
{
  public Mono<Void> run()
  {
    return Flux
        .fromIterable((Iterable<Integer>) () -> new Iterator<>()
        {
          private int i = 0;

          @Override
          public boolean hasNext()
          {
            return running;
          }

          @Override
          public Integer next()
          {
            return i++;
          }
        })
        .delayElements(Duration.ofMillis(ThreadLocalRandom.current().nextLong(500, 1500)))
        .map(i -> "Message #" + i)
        .concatMap(message -> sendMessage(chatRoom, message)
            .doOnError(throwable ->
            {
              WebClientResponseException e = (WebClientResponseException)throwable;
              log.info(
                "could not send message {} for {}: {}",
                message,
                user.getName(),
                e.getResponseBodyAsString());
            })
            .retryWhen(Retry.fixedDelay(60, Duration.ofSeconds(1))))
        .doOnNext(message ->
        {
          numSentMessages++;
          sentMessages.add(message);
          log.info(
              "{} sent a message to {}: {}",
             user,
             chatRoom,
             message);
        })
        .doOnError(throwable ->
        {
          WebClientResponseException e = (WebClientResponseException)throwable.getCause();
          log.error(
              "{} failed sending a message: {}",
              user,
              e.getResponseBodyAsString(Charset.defaultCharset()));
        })
        .takeUntil(message -> !running)
        .doOnComplete(() -> log.info("TestWriter {} is done", user))
        .then();
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
        .header("X-Shard", chatRoom.getShard().toString())
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

  final ChatRoomInfoTo chatRoom;
  final User user;
  final List<MessageTo> sentMessages = new LinkedList<>();

  volatile boolean running = true;
  @Getter
  private volatile int numSentMessages = 0;


  TestWriter(Integer port, ChatRoomInfoTo chatRoom, String username)
  {
    webClient = WebClient.create("http://localhost:" + port);
    this.chatRoom = chatRoom;
    user = new User(username);
  }
}
