package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.implementation.kafka.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.List;

import static de.juplo.kafka.chat.backend.KafkaConfigurationIT.DATA_TOPIC;
import static de.juplo.kafka.chat.backend.KafkaConfigurationIT.INFO_TOPIC;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "chat.backend.services=kafka",
        "chat.backend.kafka.client-id-PREFIX=TEST",
        "chat.backend.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "chat.backend.kafka.info-channel-topic=" + INFO_TOPIC,
        "chat.backend.kafka.data-channel-topic=" + DATA_TOPIC,
        "chat.backend.kafka.num-partitions=10",
        })
@EmbeddedKafka(
    topics = { INFO_TOPIC, DATA_TOPIC },
    partitions = 10)
@Slf4j
class KafkaConfigurationIT extends AbstractConfigurationWithShardingIT
{
  final static String INFO_TOPIC = "KAFKA_CONFIGURATION_IT_INFO_CHANNEL";
  final static String DATA_TOPIC = "KAFKA_CONFIGURATION_IT_DATA_CHANNEL";

  @MockBean
  KafkaServicesApplicationRunner kafkaServicesApplicationRunner;

  @BeforeAll
  public static void sendAndLoadStoredData(
      @Autowired KafkaTemplate<String, String> messageTemplate,
      @Autowired ConsumerTaskRunner consumerTaskRunner)
  {
    send(messageTemplate, INFO_TOPIC, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\": \"5c73531c-6fc4-426c-adcb-afc5c140a0f7\", \"shard\": 2, \"name\": \"FOO\" }", "event_chatroom_created");
    send(messageTemplate, DATA_TOPIC, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"peter\", \"text\" : \"Hallo, ich heiße Peter!\" }", "event_chatmessage_received");
    send(messageTemplate, DATA_TOPIC, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"ute\", \"text\" : \"Ich bin Ute...\" }", "event_chatmessage_received");
    send(messageTemplate, DATA_TOPIC, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 2, \"user\" : \"peter\", \"text\" : \"Willst du mit mir gehen?\" }", "event_chatmessage_received");
    send(messageTemplate, DATA_TOPIC, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"klaus\", \"text\" : \"Ja? Nein? Vielleicht??\" }", "event_chatmessage_received");

    consumerTaskRunner.executeConsumerTasks();
  }

  static void send(
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
  @AfterAll
  static void joinConsumerTasks(@Autowired ConsumerTaskRunner consumerTaskRunner)
  {
    consumerTaskRunner.joinConsumerTasks();
  }


  @TestConfiguration
  @EnableConfigurationProperties(ChatBackendProperties.class)
  @Import(KafkaServicesConfiguration.class)
  static class KafkaConfigurationITConfiguration
  {
    @Bean
    WorkAssignor dataChannelWorkAssignor(
        DataChannel dataChannel)
    {
      return consumer ->
      {
        List<TopicPartition> assignedPartitions =
            List.of(new TopicPartition(DATA_TOPIC, 2));
        consumer.assign(assignedPartitions);
        dataChannel.onPartitionsAssigned(assignedPartitions);
      };
    }
  }
}
