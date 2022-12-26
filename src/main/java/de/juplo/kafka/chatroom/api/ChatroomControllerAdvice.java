package de.juplo.kafka.chatroom.api;

import de.juplo.kafka.chatroom.domain.MessageMutationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@ControllerAdvice
public class ChatroomControllerAdvice extends ResponseEntityExceptionHandler
{
  @ExceptionHandler(MessageMutationException.class)
  public final Mono<ResponseEntity<Object>> handleException(MessageMutationException e, ServerWebExchange exchange)
  {
    final HttpStatus status = HttpStatus.BAD_REQUEST;
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, e.getMessage());
    body.setProperty("new", e.getToAdd());
    body.setProperty("existing", e.getExisting());
    return handleExceptionInternal(e, body, null, status, exchange);
  }
}
