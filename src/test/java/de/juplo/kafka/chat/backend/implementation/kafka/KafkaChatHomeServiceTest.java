package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.ChatHomeServiceWithShardsTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Clock;
import java.util.List;

import static de.juplo.kafka.chat.backend.domain.ChatHomeServiceWithShardsTest.NUM_SHARDS;
import static de.juplo.kafka.chat.backend.implementation.kafka.KafkaChatHomeServiceTest.TOPIC;


@SpringBootTest(
    classes = {
        KafkaChatHomeServiceTest.KafkaChatHomeTestConfiguration.class,
        KafkaAutoConfiguration.class,
        TaskExecutionAutoConfiguration.class,
    },
    properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "chat.backend.services=kafka",
    "chat.backend.kafka.client-id-PREFIX=TEST",
    "chat.backend.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "chat.backend.kafka.chatroom-channel-topic=" + TOPIC,
    "chat.backend.kafka.num-partitions=" + NUM_SHARDS,
})
@EmbeddedKafka(topics = { TOPIC }, partitions = 10)
@Slf4j
public class KafkaChatHomeServiceTest extends ChatHomeServiceWithShardsTest
{
  final static String TOPIC = "KAFKA_CHAT_HOME_TEST";


  @TestConfiguration
  @EnableConfigurationProperties(ChatBackendProperties.class)
  @Import(KafkaServicesConfiguration.class)
  static class KafkaChatHomeTestConfiguration
  {
    @Bean
    ConsumerTaskExecutor.WorkAssignor workAssignor(
        ChatRoomChannel chatRoomChannel)
    {
      return consumer ->
      {
        List<TopicPartition> assignedPartitions =
            List.of(new TopicPartition(TOPIC, 2));
        consumer.assign(assignedPartitions);
        chatRoomChannel.onPartitionsAssigned(assignedPartitions);
      };
    }

    @Bean
    Clock clock()
    {
      return Clock.systemDefaultZone();
    }
  }


  @BeforeAll
  public static void sendAndLoadStoredData(
      @Autowired ConsumerTaskExecutor consumerTaskExecutor,
      @Autowired KafkaTemplate<String, String> messageTemplate)
  {
    send(messageTemplate, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\": \"5c73531c-6fc4-426c-adcb-afc5c140a0f7\", \"shard\": 2, \"name\": \"FOO\" }", "command_create_chatroom");
    send(messageTemplate,"5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"peter\", \"text\" : \"Hallo, ich heiße Peter!\" }", "event_chatmessage_received");
    send(messageTemplate,"5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"ute\", \"text\" : \"Ich bin Ute...\" }", "event_chatmessage_received");
    send(messageTemplate,"5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 2, \"user\" : \"peter\", \"text\" : \"Willst du mit mir gehen?\" }", "event_chatmessage_received");
    send(messageTemplate,"5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"klaus\", \"text\" : \"Ja? Nein? Vielleicht??\" }", "event_chatmessage_received");

    consumerTaskExecutor.executeConsumerTask();
  }

  static void send(KafkaTemplate<String, String> kafkaTemplate, String key, String value, String typeId)
  {
    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, value);
    record.headers().add("__TypeId__", typeId.getBytes());
    SendResult<String, String> result = kafkaTemplate.send(record).join();
    log.info(
        "Sent {}={} to {}",
        key,
        value,
        new TopicPartition(result.getRecordMetadata().topic(), result.getRecordMetadata().partition()));
  }

  @AfterAll
  static void joinConsumerJob(@Autowired ConsumerTaskExecutor consumerTaskExecutor)
  {
    consumerTaskExecutor.joinConsumerTaskJob();
  }
}
