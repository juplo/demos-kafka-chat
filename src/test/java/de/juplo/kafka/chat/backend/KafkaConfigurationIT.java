package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.implementation.kafka.ChatRoomChannel;
import de.juplo.kafka.chat.backend.implementation.kafka.KafkaServicesApplicationRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static de.juplo.kafka.chat.backend.KafkaConfigurationIT.TOPIC;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "chat.backend.services=kafka",
        "chat.backend.kafka.client-id-PREFIX=TEST",
        "chat.backend.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "chat.backend.kafka.chatroom-channel-topic=" + TOPIC,
        "chat.backend.kafka.num-partitions=10",
        })
@EmbeddedKafka(topics = { TOPIC }, partitions = 10)
@Slf4j
class KafkaConfigurationIT extends AbstractConfigurationWithShardingIT
{
  final static String TOPIC = "KAFKA_CONFIGURATION_IT";

  static CompletableFuture<Void> CONSUMER_JOB;

  @MockBean
  KafkaServicesApplicationRunner kafkaServicesApplicationRunner;

  @BeforeAll
  public static void sendAndLoadStoredData(
      @Autowired KafkaTemplate<String, String> messageTemplate,
      @Autowired Consumer chatRoomChannelConsumer,
      @Autowired ThreadPoolTaskExecutor taskExecutor,
      @Autowired ChatRoomChannel chatRoomChannel)
  {
    send(messageTemplate, "5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\": \"5c73531c-6fc4-426c-adcb-afc5c140a0f7\", \"shard\": 2, \"name\": \"FOO\" }", "command_create_chatroom");
    send(messageTemplate,"5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"peter\", \"text\" : \"Hallo, ich heiße Peter!\" }", "event_chatmessage_received");
    send(messageTemplate,"5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"ute\", \"text\" : \"Ich bin Ute...\" }", "event_chatmessage_received");
    send(messageTemplate,"5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 2, \"user\" : \"peter\", \"text\" : \"Willst du mit mir gehen?\" }", "event_chatmessage_received");
    send(messageTemplate,"5c73531c-6fc4-426c-adcb-afc5c140a0f7","{ \"id\" : 1, \"user\" : \"klaus\", \"text\" : \"Ja? Nein? Vielleicht??\" }", "event_chatmessage_received");

    List<TopicPartition> assignedPartitions = List.of(new TopicPartition(TOPIC, 2));
    chatRoomChannelConsumer.assign(assignedPartitions);
    chatRoomChannel.onPartitionsAssigned(assignedPartitions);
    CONSUMER_JOB = taskExecutor
        .submitCompletable(chatRoomChannel)
        .exceptionally(e ->
        {
          log.error("The consumer for the ChatRoomChannel exited abnormally!", e);
          return null;
        });
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
  static void joinConsumerJob(@Autowired Consumer chatRoomChannelConsumer)
  {
    log.info("Signaling the consumer of the CahtRoomChannel to quit its work");
    chatRoomChannelConsumer.wakeup();
    log.info("Waiting for the consumer of the ChatRoomChannel to finish its work");
    CONSUMER_JOB.join();
    log.info("Joined the consumer of the ChatRoomChannel");
  }
}
