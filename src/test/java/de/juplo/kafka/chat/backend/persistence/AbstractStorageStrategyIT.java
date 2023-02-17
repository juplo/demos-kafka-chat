package de.juplo.kafka.chat.backend.persistence;

import de.juplo.kafka.chat.backend.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static pl.rzrz.assertj.reactor.Assertions.*;


@Slf4j
public abstract class AbstractStorageStrategyIT
{
  protected ChatHome chathome;
  protected ChatRoomFactory chatRoomFactory;


  protected abstract StorageStrategy getStorageStrategy();
  protected abstract StorageStrategyITConfig getConfig();

  protected void start()
  {
    StorageStrategyITConfig config = getConfig();
    chathome = new SimpleChatHome(config.getChatHomeService());
    chatRoomFactory = config.getChatRoomFactory();
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

    UUID chatRoomId = UUID.fromString("5c73531c-6fc4-426c-adcb-afc5c140a0f7");
    ChatRoom chatroom = chatRoomFactory.createChatRoom(chatRoomId, "FOO").block();
    chathome.putChatRoom(chatroom);
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

  @Test
  protected void testStoreAndRecreateParallelChatRooms()
  {
    start();

    assertThat(chathome.getChatRooms().toStream()).hasSize(0);

    UUID chatRoomAId = UUID.fromString("5c73531c-6fc4-426c-adcb-afc5c140a0f7");
    ChatRoom chatroomA = chatRoomFactory.createChatRoom(chatRoomAId, "FOO").block();
    chathome.putChatRoom(chatroomA);
    Message ma1 = chatroomA.addMessage(1l,"peter", "Hallo, ich heiße Peter!").block();
    Message ma2 = chatroomA.addMessage(1l, "ute", "Ich bin Ute...").block();
    Message ma3 = chatroomA.addMessage(2l, "peter", "Willst du mit mir gehen?").block();
    Message ma4 = chatroomA.addMessage(1l, "klaus", "Ja? Nein? Vielleicht??").block();

    UUID chatRoomBId = UUID.fromString("8763dfdc-4dda-4a74-bea4-4b389177abea");
    ChatRoom chatroomB = chatRoomFactory.createChatRoom(chatRoomBId, "BAR").block();
    chathome.putChatRoom(chatroomB);
    Message mb1 = chatroomB.addMessage(1l,"peter", "Hallo, ich heiße Uwe!").block();
    Message mb2 = chatroomB.addMessage(1l, "ute", "Ich bin Ute...").block();
    Message mb3 = chatroomB.addMessage(1l, "klaus", "Willst du mit mir gehen?").block();
    Message mb4 = chatroomB.addMessage(2l, "peter", "Hä? Was jetzt?!? Isch glohb isch höb ühn däjah vüh...").block();

    assertThat(chathome.getChatRooms().toStream()).containsExactlyInAnyOrderElementsOf(List.of(chatroomA, chatroomB));
    assertThat(chathome.getChatRoom(chatroomA.getId())).emitsExactly(chatroomA);
    assertThat(chathome
        .getChatRoom(chatroomA.getId())
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(ma1, ma2, ma3, ma4);
    assertThat(chathome.getChatRoom(chatroomB.getId())).emitsExactly(chatroomB);
    assertThat(chathome
        .getChatRoom(chatroomB.getId())
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(mb1, mb2, mb3, mb4);

    stop();
    start();

    assertThat(chathome.getChatRooms().toStream()).containsExactlyInAnyOrderElementsOf(List.of(chatroomA, chatroomB));
    assertThat(chathome.getChatRoom(chatroomA.getId())).emitsExactly(chatroomA);
    assertThat(chathome
        .getChatRoom(chatroomA.getId())
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(ma1, ma2, ma3, ma4);
    assertThat(chathome.getChatRoom(chatroomB.getId())).emitsExactly(chatroomB);
    assertThat(chathome
        .getChatRoom(chatroomB.getId())
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(mb1, mb2, mb3, mb4);
  }


  interface StorageStrategyITConfig
  {
    ChatHomeService getChatHomeService();
    ChatRoomFactory getChatRoomFactory();
  }
}
