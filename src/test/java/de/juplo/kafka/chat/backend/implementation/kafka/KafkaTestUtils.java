package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Clock;
import java.util.List;


@Slf4j
public class KafkaTestUtils
{
  @TestConfiguration
  @EnableConfigurationProperties(ChatBackendProperties.class)
  @Import(KafkaServicesConfiguration.class)
  public static class KafkaTestConfiguration
  {
    @Bean
    public WorkAssignor dataChannelWorkAssignor(
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

    @Bean
    public Clock clock()
    {
      return Clock.systemDefaultZone();
    }
  }


  public static void sendAndLoadStoredData(
      KafkaTemplate<String, String> messageTemplate,
      String infoTopic,
      String dataTopic,
      ConsumerTaskRunner consumerTaskRunner)
  {
    send(messageTemplate, infoTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\": \"5c73531c-6fc4-426c-adcb-afc5c140a0f7\", \"shard\": 2, \"name\": \"FOO\" }", "event_chatroom_created");
    send(messageTemplate, dataTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"peter\", \"text\" : \"Hallo, ich heiße Peter!\" }", "event_chatmessage_received");
    send(messageTemplate, dataTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"ute\", \"text\" : \"Ich bin Ute...\" }", "event_chatmessage_received");
    send(messageTemplate, dataTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 2, \"user\" : \"peter\", \"text\" : \"Willst du mit mir gehen?\" }", "event_chatmessage_received");
    send(messageTemplate, dataTopic, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"klaus\", \"text\" : \"Ja? Nein? Vielleicht??\" }", "event_chatmessage_received");

    consumerTaskRunner.executeConsumerTasks();
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

  public static void joinConsumerTasks(ConsumerTaskRunner consumerTaskRunner) throws InterruptedException
  {
    consumerTaskRunner.joinConsumerTasks();
  }
}
