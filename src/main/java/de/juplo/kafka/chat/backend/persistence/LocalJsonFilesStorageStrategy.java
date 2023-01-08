package de.juplo.kafka.chat.backend.persistence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.api.ChatRoomTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;


@RequiredArgsConstructor
@Slf4j
public class LocalJsonFilesStorageStrategy implements StorageStrategy
{
  public static final String CHATROOMS_FILENAME = "chatrooms.json";


  private final Path storagePath;
  private final ObjectMapper mapper;
  private final InMemoryChatHomeService service;


  @Override
  public void writeChatrooms(Flux<ChatRoom> chatroomFlux)
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
              ChatRoomTo chatroomTo = ChatRoomTo.from(chatroom);
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
  public Flux<ChatRoom> readChatrooms()
  {
    JavaType type = mapper.getTypeFactory().constructType(ChatRoomTo.class);
    return Flux
        .from(new JsonFilePublisher<ChatRoomTo>(chatroomsPath(), mapper, type))
        .log()
        .map(chatRoomTo ->
        {
          InMemoryChatroomService chatroomService =
              new InMemoryChatroomService(readMessages(chatRoomTo));
          return service.restoreChatroom(
              chatRoomTo.getId(),
              chatRoomTo.getName(),
              chatroomService);
        });
  }

  @Override
  public void writeMessages(ChatRoomTo chatroomTo, Flux<Message> messageFlux)
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
  public Flux<Message> readMessages(ChatRoomTo chatroomTo)
  {
    JavaType type = mapper.getTypeFactory().constructType(MessageTo.class);
    return Flux
        .from(new JsonFilePublisher<MessageTo>(chatroomPath(chatroomTo), mapper, type))
        .log()
        .map(MessageTo::toMessage);
  }

  Path chatroomsPath()
  {
    return storagePath.resolve(Path.of(CHATROOMS_FILENAME));
  }

  Path chatroomPath(ChatRoomTo chatroomTo)
  {
    return storagePath.resolve(Path.of(chatroomTo.getId().toString() + ".json"));
  }
}
