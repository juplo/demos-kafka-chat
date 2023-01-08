package de.juplo.kafka.chat.backend.api;

import de.juplo.kafka.chat.backend.domain.MessageMutationException;
import de.juplo.kafka.chat.backend.domain.UnknownChatroomException;
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

    problem.setProperty("mutatedMessage", MessageTo.from(e.getMutated()));

    problem.setProperty("existingMessage", MessageTo.from(e.getExisting()));

    return problem;
  }
}
