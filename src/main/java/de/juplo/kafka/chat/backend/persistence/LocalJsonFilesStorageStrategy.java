package de.juplo.kafka.chat.backend.persistence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.api.ChatroomTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import de.juplo.kafka.chat.backend.domain.Chatroom;
import de.juplo.kafka.chat.backend.domain.ChatroomFactory;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.domain.MessageMutationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;


@RequiredArgsConstructor
@Slf4j
public class LocalJsonFilesStorageStrategy implements StorageStrategy
{
  public static final String CHATROOMS_FILENAME = "chatrooms.json";


  private final Path storagePath;
  private final ObjectMapper mapper;
  private final ChatroomFactory chatroomFactory;


  @Override
  public void writeChatrooms(Flux<Chatroom> chatroomFlux)
  {
    Path path = chatroomsPath();
    log.info("Writing chatrooms to {}", path);
    try
    {
      Files.createDirectories(storagePath);

      JsonGenerator generator =
          mapper
              .getFactory()
              .createGenerator(Files.newBufferedWriter(path, CREATE, TRUNCATE_EXISTING));

      chatroomFlux
          .log()
          .doFirst(() ->
          {
            try
            {
              generator.useDefaultPrettyPrinter();
              generator.writeStartArray();
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          })
          .doOnTerminate(() ->
          {
            try
            {
              generator.writeEndArray();
              generator.close();
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          })
          .subscribe(chatroom ->
          {
            try
            {
              ChatroomTo chatroomTo = ChatroomTo.from(chatroom);
              generator.writeObject(chatroomTo);
              writeMessages(chatroomTo, chatroom.getMessages());
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          });
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Flux<Chatroom> readChatrooms()
  {
    Path path = chatroomsPath();
    log.info("Reading chatrooms from {}", path);
    try
    {
      JsonParser parser =
          mapper
              .getFactory()
              .createParser(Files.newBufferedReader(path));

      if (parser.nextToken() != JsonToken.START_ARRAY)
        throw new IllegalStateException("Expected content to be an array");

      Sinks.Many<ChatroomTo> many = Sinks.many().unicast().onBackpressureBuffer();

      while (parser.nextToken() != JsonToken.END_ARRAY)
      {
        many
            .tryEmitNext(mapper.readValue(parser, ChatroomTo.class))
            .orThrow();
      }

      many.tryEmitComplete().orThrow();

      return many
          .asFlux()
          .map(chatroomTo ->
          {
            LinkedHashMap<Message.MessageKey, Message> messages =
                readMessages(chatroomTo)
                    .collect(Collectors.toMap(
                        Message::getKey,
                        Function.identity(),
                        (existing, message) ->
                        {
                          if (!message.equals(existing))
                            throw new MessageMutationException(message, existing);
                          return existing;
                        },
                        LinkedHashMap::new))
                    .block();
            InMemoryPersistenceStrategy strategy = new InMemoryPersistenceStrategy(messages);
            return chatroomFactory.restoreChatroom(chatroomTo.getId(), chatroomTo.getName(), strategy);
          });
    }
    catch (NoSuchFileException e)
    {
      log.info("{} does not exist - starting with empty ChatHome", path);
      return Flux.empty();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeMessages(ChatroomTo chatroomTo, Flux<Message> messageFlux)
  {
    Path path = chatroomPath(chatroomTo);
    log.info("Writing messages for {} to {}", chatroomTo, path);
    try
    {
      Files.createDirectories(storagePath);

      JsonGenerator generator =
          mapper
              .getFactory()
              .createGenerator(Files.newBufferedWriter(path, CREATE, TRUNCATE_EXISTING));

      messageFlux
          .log()
          .doFirst(() ->
          {
            try
            {
              generator.useDefaultPrettyPrinter();
              generator.writeStartArray();
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          })
          .doOnTerminate(() ->
          {
            try
            {
              generator.writeEndArray();
              generator.close();
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          })
          .subscribe(message ->
          {
            try
            {
              MessageTo messageTo = MessageTo.from(message);
              generator.writeObject(messageTo);
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          });
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Flux<Message> readMessages(ChatroomTo chatroomTo)
  {
    Path path = chatroomPath(chatroomTo);
    log.info("Reading messages for {} from {}", chatroomTo, path);
    try
    {
      JsonParser parser =
          mapper
              .getFactory()
              .createParser(Files.newBufferedReader(path));

      if (parser.nextToken() != JsonToken.START_ARRAY)
        throw new IllegalStateException("Expected content to be an array");

      Sinks.Many<Message> many = Sinks.many().unicast().onBackpressureBuffer();

      while (parser.nextToken() != JsonToken.END_ARRAY)
      {
        many
            .tryEmitNext(mapper.readValue(parser, MessageTo.class).toMessage())
            .orThrow();
      }

      many.tryEmitComplete().orThrow();

      return many.asFlux();
    }
    catch (NoSuchFileException e)
    {
      log.info(
          "{} does not exist - starting with empty chat for {}",
          path,
          chatroomTo);
      return Flux.empty();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  Path chatroomsPath()
  {
    return storagePath.resolve(Path.of(CHATROOMS_FILENAME));
  }

  Path chatroomPath(ChatroomTo chatroomTo)
  {
    return storagePath.resolve(Path.of(chatroomTo.getId().toString() + ".json"));
  }
}
