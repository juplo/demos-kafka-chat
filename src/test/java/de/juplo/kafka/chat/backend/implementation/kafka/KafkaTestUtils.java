package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ShardingPublisherStrategy;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.publisher.Mono;

import java.util.List;


@Slf4j
public abstract class KafkaTestUtils
{
  public static void initKafkaSetup(
      String infoTopic,
      String dataTopic,
      KafkaTemplate<String, String> messageTemplate,
      ChannelTaskExecutor infoChannelTaskExecutor,
      ChannelTaskExecutor dataChannelTaskExecutor)
  {
    KafkaTestUtils.sendAndLoadStoredData(
        messageTemplate,
        infoTopic,
        dataTopic);

    // The initialization of the channels must happen,
    // after the messages were sent into the topics of the
    // test-cluster.
    // Otherwise, the initial loading of the data might be
    // completed, before these messages arrive, so that
    // they are ignored and the state is never restored.
    infoChannelTaskExecutor.executeChannelTask();
    dataChannelTaskExecutor.executeChannelTask();
  }

  public static class KafkaTestConfiguration
  {
    @Bean
    ShardingPublisherStrategy shardingPublisherStrategy()
    {
      return shard -> Mono.just("MOCKED!");
    }

    @Bean
    WorkAssignor dataChannelWorkAssignor(
        ChatBackendProperties properties,
        DataChannel dataChannel)
    {
      return consumer ->
      {
        List<TopicPartition> assignedPartitions =
            List.of(new TopicPartition(properties.getKafka().getDataChannelTopic(), 2));
        consumer.assign(assignedPartitions);
        dataChannel.onPartitionsAssigned(assignedPartitions);
      };
    }

    /**
     * The definition of this bean has to be overruled, so
     * that the configuration of the `initMethod`, which
     * has to be called explicitly, _after_ the messages
     * were sent to and received by the test-culster, can
     * be dropped.
     */
    @Bean(destroyMethod = "join")
    ChannelTaskExecutor infoChannelTaskExecutor(
        ThreadPoolTaskExecutor taskExecutor,
        InfoChannel infoChannel,
        Consumer<String, AbstractMessageTo> infoChannelConsumer,
        WorkAssignor infoChannelWorkAssignor)
    {
      return new ChannelTaskExecutor(
          taskExecutor,
          infoChannel,
          infoChannelConsumer,
          infoChannelWorkAssignor);
    }

    /**
     * The definition of this bean has to be overruled, so
     * that the configuration of the `initMethod`, which
     * has to be called explicitly, _after_ the messages
     * were sent to and received by the test-culster, can
     * be dropped.
     */
    @Bean(destroyMethod = "join")
    ChannelTaskExecutor dataChannelTaskExecutor(
        ThreadPoolTaskExecutor taskExecutor,
        DataChannel dataChannel,
        Consumer<String, AbstractMessageTo> dataChannelConsumer,
        WorkAssignor dataChannelWorkAssignor)
    {
      return new ChannelTaskExecutor(
          taskExecutor,
          dataChannel,
          dataChannelConsumer,
          dataChannelWorkAssignor);
    }
  }


  private static void sendAndLoadStoredData(
      KafkaTemplate<String, String> messageTemplate,
      String infoTopic,
      String dataTopic)
  {
    send(messageTemplate, infoTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\": \"5c73531c-6fc4-426c-adcb-afc5c140a0f7\", \"shard\": 2, \"name\": \"FOO\" }", "event_chatroom_created");
    send(messageTemplate, dataTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"peter\", \"text\" : \"Hallo, ich heiße Peter!\" }", "event_chatmessage_received");
    send(messageTemplate, dataTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"ute\", \"text\" : \"Ich bin Ute...\" }", "event_chatmessage_received");
    send(messageTemplate, dataTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 2, \"user\" : \"peter\", \"text\" : \"Willst du mit mir gehen?\" }", "event_chatmessage_received");
    send(messageTemplate, dataTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"klaus\", \"text\" : \"Ja? Nein? Vielleicht??\" }", "event_chatmessage_received");
  }

  private static void send(
      KafkaTemplate<String, String> kafkaTemplate,
      String topic,
      String key,
      String value,
      String typeId)
  {
    ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
    record.headers().add("__TypeId__", typeId.getBytes());
    SendResult<String, String> result = kafkaTemplate.send(record).join();
    log.info(
        "Sent {}={} to {}",
        key,
        value,
        new TopicPartition(result.getRecordMetadata().topic(), result.getRecordMetadata().partition()));
  }
}
