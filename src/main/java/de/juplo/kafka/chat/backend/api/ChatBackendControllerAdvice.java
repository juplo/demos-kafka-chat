package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.exceptions.InvalidUsernameException;
import de.juplo.kafka.chat.backend.domain.exceptions.MessageMutationException;
import de.juplo.kafka.chat.backend.domain.exceptions.ShardNotOwnedException;
import de.juplo.kafka.chat.backend.domain.exceptions.UnknownChatroomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;


@ControllerAdvice
public class ChatBackendControllerAdvice
{
  @Value("${server.context-path:/}")
  String contextPath;

  @ExceptionHandler(UnknownChatroomException.class)
  public final ProblemDetail handleException(
      UnknownChatroomException e,
      ServerWebExchange exchange,
      UriComponentsBuilder uriComponentsBuilder)
  {
    final HttpStatus status = HttpStatus.NOT_FOUND;
    ProblemDetail problem = ProblemDetail.forStatus(status);

    problem.setProperty("timestamp", new Date());

    problem.setProperty("requestId", exchange.getRequest().getId());

    problem.setType(uriComponentsBuilder.replacePath(contextPath).path("/problem/unknown-chatroom").build().toUri());
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(status.getReasonPhrase());
    stringBuilder.append(" - ");
    stringBuilder.append(e.getMessage());
    problem.setTitle(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("Chatroom unknown: ");
    stringBuilder.append(e.getChatroomId());
    problem.setDetail(stringBuilder.toString());

    problem.setProperty("chatroomId", e.getChatroomId());
    problem.setProperty("shard", e.getShard());
    problem.setProperty("ownedShards", e.getOwnedShards());

    return problem;
  }

  @ExceptionHandler(ShardNotOwnedException.class)
  public final ProblemDetail handleException(
      ShardNotOwnedException e,
      ServerWebExchange exchange,
      UriComponentsBuilder uriComponentsBuilder)
  {
    final HttpStatus status = HttpStatus.NOT_FOUND;
    ProblemDetail problem = ProblemDetail.forStatus(status);

    problem.setProperty("timestamp", new Date());

    problem.setProperty("requestId", exchange.getRequest().getId());

    problem.setType(uriComponentsBuilder.replacePath(contextPath).path("/problem/shard-not-owned").build().toUri());
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(status.getReasonPhrase());
    stringBuilder.append(" - ");
    stringBuilder.append(e.getMessage());
    problem.setTitle(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("Shard not owned: ");
    stringBuilder.append(e.getShard());
    problem.setDetail(stringBuilder.toString());

    problem.setProperty("shard", e.getShard());

    return problem;
  }

  @ExceptionHandler(MessageMutationException.class)
  public final ProblemDetail handleException(
      MessageMutationException e,
      ServerWebExchange exchange,
      UriComponentsBuilder uriComponentsBuilder)
  {
    final HttpStatus status = HttpStatus.BAD_REQUEST;
    ProblemDetail problem = ProblemDetail.forStatus(status);

    problem.setProperty("timestamp", new Date());

    problem.setProperty("requestId", exchange.getRequest().getId());

    problem.setType(uriComponentsBuilder.replacePath(contextPath).path("/problem/message-mutation").build().toUri());
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(status.getReasonPhrase());
    stringBuilder.append(" - ");
    stringBuilder.append(e.getMessage());
    problem.setTitle(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("The existing message with user=");
    stringBuilder.append(e.getExisting().getUsername());
    stringBuilder.append(" and id=");
    stringBuilder.append(e.getExisting().getId());
    stringBuilder.append(" cannot be mutated!");
    problem.setDetail(stringBuilder.toString());

    problem.setProperty("existingMessage", MessageTo.from(e.getExisting()));

    problem.setProperty("mutatedText", e.getMutatedText());

    return problem;
  }

  @ExceptionHandler(InvalidUsernameException.class)
  public final ProblemDetail handleException(
      InvalidUsernameException e,
      ServerWebExchange exchange,
      UriComponentsBuilder uriComponentsBuilder)
  {
    final HttpStatus status = HttpStatus.BAD_REQUEST;
    ProblemDetail problem = ProblemDetail.forStatus(status);

    problem.setProperty("timestamp", new Date());

    problem.setProperty("requestId", exchange.getRequest().getId());

    problem.setType(uriComponentsBuilder.replacePath(contextPath).path("/problem/invalid-username").build().toUri());
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(status.getReasonPhrase());
    stringBuilder.append(" - ");
    stringBuilder.append(e.getMessage());
    problem.setTitle(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("Invalid username: ");
    stringBuilder.append(e.getUsername());
    stringBuilder.append(
        "! A valid username must consist of at at least two letters and " +
        "must only contain lower case letters a-z, numbers and dashes");
    problem.setDetail(stringBuilder.toString());

    problem.setProperty("username", e.getUsername());

    return problem;
  }
}
