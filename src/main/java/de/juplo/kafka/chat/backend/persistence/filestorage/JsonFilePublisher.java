package de.juplo.kafka.chat.backend.persistence.filestorage;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;


@RequiredArgsConstructor
@Slf4j
public class JsonFilePublisher<T> implements Publisher<T>
{
  private final Path path;
  private final ObjectMapper mapper;
  private final JavaType type;


  @Override
  public void subscribe(Subscriber<? super T> subscriber)
  {
    log.info("Reading chatrooms from {}", path);

    try
    {
      JsonParser parser =
          mapper.getFactory().createParser(Files.newBufferedReader(path));

      if (parser.nextToken() != JsonToken.START_ARRAY)
      {
        throw new IllegalStateException("Expected content to be an array");
      }

      subscriber.onSubscribe(new JsonFileSubscription(subscriber, parser));
    }
    catch (NoSuchFileException e)
    {
      log.info("{} does not exist - starting with empty ChatHome", path);
      subscriber.onSubscribe(new ReplaySubscription(subscriber, List.of()));
    }
    catch (IOException | IllegalStateException e)
    {
      subscriber.onSubscribe(new ReplaySubscription(subscriber, List.of((s -> s.onError(e)))));
    }
  }

  @RequiredArgsConstructor
  private class JsonFileSubscription implements Subscription
  {
    private final Subscriber<? super T> subscriber;
    private final JsonParser parser;

    @Override
    public void request(long requested)
    {
      try
      {
        while (requested > 0 && parser.nextToken() != JsonToken.END_ARRAY)
        {
          subscriber.onNext(mapper.readValue(parser, type));
          requested--;
        }

        if (requested > 0)
          subscriber.onComplete();
      }
      catch (IOException e)
      {
        subscriber.onError(e);
      }
    }

    @Override
    public void cancel() {}
  }

  private class ReplaySubscription implements Subscription
  {
    private final Subscriber<? super T> subscriber;
    private final Iterator<Consumer<Subscriber<? super T>>> iterator;

    ReplaySubscription(
        Subscriber<? super T> subscriber,
        Iterable<Consumer<Subscriber<? super T>>> actions)
    {
      this.subscriber = subscriber;
      this.iterator = actions.iterator();
    }

    @Override
    public void request(long requested)
    {
      while (requested > 0 && iterator.hasNext())
      {
        iterator.next().accept(subscriber);
        requested--;
      }

      if (requested > 0)
        subscriber.onComplete();
    }

    @Override
    public void cancel() {}
  }
}
