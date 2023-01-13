package de.juplo.kafka.chat.backend.domain;

public interface ChatHomeFactory
{
  ChatHome createChatHome(int shard);
}
