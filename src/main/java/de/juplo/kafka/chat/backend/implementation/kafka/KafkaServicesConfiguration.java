package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.data.EventChatMessageReceivedTo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.info.EventChatRoomCreated;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
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
  ConsumerTaskRunner consumerTaskRunner(
      ConsumerTaskExecutor infoChannelConsumerTaskExecutor,
      ConsumerTaskExecutor dataChannelConsumerTaskExecutor)
  {
    return new ConsumerTaskRunner(
        infoChannelConsumerTaskExecutor,
        dataChannelConsumerTaskExecutor);
  }

  @Bean
  ConsumerTaskExecutor infoChannelConsumerTaskExecutor(
      ThreadPoolTaskExecutor taskExecutor,
      InfoChannel infoChannel,
      Consumer<String, AbstractMessageTo> infoChannelConsumer,
      WorkAssignor infoChannelWorkAssignor)
  {
    return new ConsumerTaskExecutor(
        taskExecutor,
        infoChannel,
        infoChannelConsumer,
        infoChannelWorkAssignor);
  }

  @Bean
  WorkAssignor infoChannelWorkAssignor(ChatBackendProperties properties)
  {
    return consumer ->
    {
      String topic = properties.getKafka().getInfoChannelTopic();
      List<TopicPartition> partitions = consumer
          .partitionsFor(topic)
          .stream()
          .map(partitionInfo ->
              new TopicPartition(topic, partitionInfo.partition()))
          .toList();
      consumer.assign(partitions);
    };
  }

  @Bean
  ConsumerTaskExecutor dataChannelConsumerTaskExecutor(
      ThreadPoolTaskExecutor taskExecutor,
      DataChannel dataChannel,
      Consumer<String, AbstractMessageTo> dataChannelConsumer,
      WorkAssignor dataChannelWorkAssignor)
  {
    return new ConsumerTaskExecutor(
        taskExecutor,
        dataChannel,
        dataChannelConsumer,
        dataChannelWorkAssignor);
  }

  @Bean
  WorkAssignor dataChannelWorkAssignor(
      ChatBackendProperties properties,
      DataChannel dataChannel)
  {
    return consumer ->
    {
      List<String> topics =
          List.of(properties.getKafka().getDataChannelTopic());
      consumer.subscribe(topics, dataChannel);
    };
  }

  @Bean
    ChatHomeService kafkaChatHome(
      ChatBackendProperties properties,
      InfoChannel infoChannel,
      DataChannel dataChannel)
  {
    return new KafkaChatHomeService(
        properties.getKafka().getNumPartitions(),
        infoChannel,
        dataChannel);
  }

  @Bean
  InfoChannel infoChannel(
      ChatBackendProperties properties,
      Producer<String, AbstractMessageTo> producer,
      Consumer<String, AbstractMessageTo> infoChannelConsumer)
  {
    return new InfoChannel(
        properties.getKafka().getInfoChannelTopic(),
        producer,
        infoChannelConsumer);
  }

  @Bean
  DataChannel dataChannel(
      ChatBackendProperties properties,
      Producer<String, AbstractMessageTo> producer,
      Consumer<String, AbstractMessageTo> dataChannelConsumer,
      ZoneId zoneId,
      Clock clock,
      InfoChannel infoChannel)
  {
    return new DataChannel(
        properties.getKafka().getDataChannelTopic(),
        producer,
        dataChannelConsumer,
        zoneId,
        properties.getKafka().getNumPartitions(),
        properties.getChatroomBufferSize(),
        clock,
        infoChannel);
  }

  @Bean
  Producer<String, AbstractMessageTo>  producer(
      Properties defaultProducerProperties,
      ChatBackendProperties chatBackendProperties,
      StringSerializer stringSerializer,
      JsonSerializer<AbstractMessageTo> messageSerializer)
  {
    Map<String, Object> properties = new HashMap<>();
    defaultProducerProperties.forEach((key, value) -> properties.put(key.toString(), value));
    properties.put(
        ProducerConfig.CLIENT_ID_CONFIG,
        chatBackendProperties.getKafka().getClientIdPrefix() + "_PRODUCER");
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
  Consumer<String, AbstractMessageTo>  infoChannelConsumer(
      Properties defaultConsumerProperties,
      ChatBackendProperties chatBackendProperties,
      StringDeserializer stringDeserializer,
      JsonDeserializer<AbstractMessageTo> messageDeserializer)
  {
    Map<String, Object> properties = new HashMap<>();
    defaultConsumerProperties.forEach((key, value) -> properties.put(key.toString(), value));
    properties.put(
        ConsumerConfig.CLIENT_ID_CONFIG,
        chatBackendProperties.getKafka().getClientIdPrefix() + "_INFO_CHANNEL_CONSUMER");
    properties.put(
        ConsumerConfig.GROUP_ID_CONFIG,
        "info_channel");
    return new KafkaConsumer<>(
        properties,
        stringDeserializer,
        messageDeserializer);
  }

  @Bean
  Consumer<String, AbstractMessageTo>  dataChannelConsumer(
      Properties defaultConsumerProperties,
      ChatBackendProperties chatBackendProperties,
      StringDeserializer stringDeserializer,
      JsonDeserializer<AbstractMessageTo> messageDeserializer)
  {
    Map<String, Object> properties = new HashMap<>();
    defaultConsumerProperties.forEach((key, value) -> properties.put(key.toString(), value));
    properties.put(
        ConsumerConfig.CLIENT_ID_CONFIG,
        chatBackendProperties.getKafka().getClientIdPrefix() + "_DATA_CHANNEL_CONSUMER");
    properties.put(
        ConsumerConfig.GROUP_ID_CONFIG,
        "data_channel");
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
        "event_chatroom_created:" +  EventChatRoomCreated.class.getCanonicalName() + "," +
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
