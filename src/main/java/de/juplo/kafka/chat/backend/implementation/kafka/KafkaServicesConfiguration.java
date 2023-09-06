package de.juplo.kafka.chat.backend.persistence.kafka;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.persistence.kafka.messages.AbstractMessageTo;
import de.juplo.kafka.chat.backend.persistence.kafka.messages.CommandCreateChatRoomTo;
import de.juplo.kafka.chat.backend.persistence.kafka.messages.EventChatMessageReceivedTo;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "services",
    havingValue = "kafka")
@Configuration
public class KafkaServicesConfiguration
{
  @Bean
  ChatHomeService kafkaChatHome(
      ChatBackendProperties properties,
      ChatRoomChannel chatRoomChannel)
  {
    return new KafkaChatHomeService(
        properties.getKafka().getNumPartitions(),
        chatRoomChannel);
  }

  @Bean
  ChatRoomChannel chatRoomChannel(
      ChatBackendProperties properties,
      Producer<String, AbstractMessageTo> chatRoomChannelProducer,
      Consumer<String, AbstractMessageTo> chatRoomChannelConsumer,
      ZoneId zoneId,
      Clock clock)
  {
    return new ChatRoomChannel(
        properties.getKafka().getChatRoomChannelTopic(),
        chatRoomChannelProducer,
        chatRoomChannelConsumer,
        zoneId,
        properties.getKafka().getNumPartitions(),
        properties.getChatroomBufferSize(),
        clock);
  }

  @Bean
  Producer<String, AbstractMessageTo>  chatRoomChannelProducer(
      Properties defaultProducerProperties,
      ChatBackendProperties chatBackendProperties,
      StringSerializer stringSerializer,
      JsonSerializer<AbstractMessageTo> messageSerializer)
  {
    Map<String, Object> properties = new HashMap<>();
    defaultProducerProperties.forEach((key, value) -> properties.put(key.toString(), value));
    properties.put(
        ProducerConfig.CLIENT_ID_CONFIG,
        chatBackendProperties.getKafka().getClientIdPrefix() + "_CHATROOM_CHANNEL_PRODUCER");
    return new KafkaProducer<>(
        properties,
        stringSerializer,
        messageSerializer);
  }

  @Bean
  StringSerializer stringSerializer()
  {
    return new StringSerializer();
  }

  @Bean
  JsonSerializer<AbstractMessageTo> chatMessageSerializer(String typeMappings)
  {
    JsonSerializer<AbstractMessageTo> serializer = new JsonSerializer<>();
    serializer.configure(
        Map.of(
            JsonSerializer.TYPE_MAPPINGS, typeMappings),
        false);
    return serializer;
  }

  @Bean
  Consumer<String, AbstractMessageTo>  chatRoomChannelConsumer(
      Properties defaultConsumerProperties,
      ChatBackendProperties chatBackendProperties,
      StringDeserializer stringDeserializer,
      JsonDeserializer<AbstractMessageTo> messageDeserializer)
  {
    Map<String, Object> properties = new HashMap<>();
    defaultConsumerProperties.forEach((key, value) -> properties.put(key.toString(), value));
    properties.put(
        ConsumerConfig.CLIENT_ID_CONFIG,
        chatBackendProperties.getKafka().getClientIdPrefix() + "_CHATROOM_CHANNEL_CONSUMER");
    properties.put(
        ConsumerConfig.GROUP_ID_CONFIG,
        "chatroom_channel");
    return new KafkaConsumer<>(
        properties,
        stringDeserializer,
        messageDeserializer);
  }

  @Bean
  StringDeserializer stringDeserializer()
  {
    return new StringDeserializer();
  }

  @Bean
  JsonDeserializer<AbstractMessageTo> chatMessageDeserializer(String typeMappings)
  {
    JsonDeserializer<AbstractMessageTo> deserializer = new JsonDeserializer<>();
    deserializer.configure(
        Map.of(
            JsonDeserializer.TRUSTED_PACKAGES, getClass().getPackageName(),
            JsonDeserializer.TYPE_MAPPINGS, typeMappings),
        false );
    return deserializer;
  }

  @Bean
  String typeMappings ()
  {
    return
        "command_create_chatroom:" +  CommandCreateChatRoomTo.class.getCanonicalName() + "," +
        "event_chatmessage_received:" + EventChatMessageReceivedTo.class.getCanonicalName();
  }

  @Bean
  Properties defaultProducerProperties(ChatBackendProperties chatBackendProperties)
  {
    Properties properties = new Properties();
    properties.setProperty(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        chatBackendProperties.getKafka().getBootstrapServers());
    return properties;
  }

  @Bean
  Properties defaultConsumerProperties(ChatBackendProperties chatBackendProperties)
  {
    Properties properties = new Properties();
    properties.setProperty(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        chatBackendProperties.getKafka().getBootstrapServers());
    properties.setProperty(
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
        "false");
    properties.setProperty(
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        "earliest");
    return properties;
  }

  @Bean
  ZoneId zoneId()
  {
    return ZoneId.systemDefault();
  }
}
