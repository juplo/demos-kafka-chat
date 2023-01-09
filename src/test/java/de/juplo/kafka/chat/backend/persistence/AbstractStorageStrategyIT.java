package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.Message;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static pl.rzrz.assertj.reactor.Assertions.*;


@Slf4j
public abstract class AbstractStorageStrategyIT
{
  protected ChatHome chathome;


  protected abstract StorageStrategy getStorageStrategy();
  protected abstract Supplier<ChatHomeService> getChatHomeServiceSupplier();

  protected void start()
  {
    chathome = new ChatHome(getChatHomeServiceSupplier().get());
  }

  protected void stop()
  {
    getStorageStrategy().write(chathome.getChatRooms());
  }

  @Test
  protected void testStoreAndRecreate()
  {
    start();

    assertThat(chathome.getChatRooms().toStream()).hasSize(0);

    ChatRoom chatroom = chathome.createChatroom("FOO").block();
    Message m1 = chatroom.addMessage(1l,"peter", "Hallo, ich heiße Peter!").block();
    Message m2 = chatroom.addMessage(1l, "ute", "Ich bin Ute...").block();
    Message m3 = chatroom.addMessage(2l, "peter", "Willst du mit mir gehen?").block();
    Message m4 = chatroom.addMessage(1l, "klaus", "Ja? Nein? Vielleicht??").block();

    assertThat(chathome.getChatRooms().toStream()).containsExactlyElementsOf(List.of(chatroom));
    assertThat(chathome.getChatRoom(chatroom.getId())).emitsExactly(chatroom);
    assertThat(chathome
        .getChatRoom(chatroom.getId())
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(m1, m2, m3, m4);

    stop();
    start();

    assertThat(chathome.getChatRooms().toStream()).containsExactlyElementsOf(List.of(chatroom));
    assertThat(chathome.getChatRoom(chatroom.getId())).emitsExactly(chatroom);
    assertThat(chathome
        .getChatRoom(chatroom.getId())
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(m1, m2, m3, m4);
  }
}
