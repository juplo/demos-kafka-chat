package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.domain.ChatHomeServiceWithShardsTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static de.juplo.kafka.chat.backend.domain.ChatHomeServiceWithShardsTest.NUM_SHARDS;
import static de.juplo.kafka.chat.backend.implementation.kafka.KafkaChatHomeServiceTest.DATA_TOPIC;
import static de.juplo.kafka.chat.backend.implementation.kafka.KafkaChatHomeServiceTest.INFO_TOPIC;


@ContextConfiguration(classes = {
        KafkaTestUtils.KafkaTestConfiguration.class,
        KafkaAutoConfiguration.class,
        TaskExecutionAutoConfiguration.class,
    })
@TestPropertySource(properties = {
        "chat.backend.services=kafka",
        "chat.backend.kafka.client-id-PREFIX=TEST",
        "chat.backend.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "chat.backend.kafka.info-channel-topic=" + INFO_TOPIC,
        "chat.backend.kafka.data-channel-topic=" + DATA_TOPIC,
        "chat.backend.kafka.num-partitions=" + NUM_SHARDS,
})
@EmbeddedKafka(
    topics = { INFO_TOPIC, DATA_TOPIC },
    partitions = NUM_SHARDS)
@Slf4j
public class KafkaChatHomeServiceTest extends ChatHomeServiceWithShardsTest
{
  final static String INFO_TOPIC = "KAFKA_CHAT_HOME_TEST_INFO";
  final static String DATA_TOPIC = "KAFKA_CHAT_HOME_TEST_DATA";


  @BeforeAll
  static void sendAndLoadStoredData(
      @Autowired KafkaTemplate<String, String> messageTemplate,
      @Autowired ChannelTaskExecutor infoChannelTaskExecutor,
      @Autowired ChannelTaskExecutor dataChannelTaskExecutor)
  {
    KafkaTestUtils.initKafkaSetup(
        INFO_TOPIC,
        DATA_TOPIC,
        messageTemplate,
        infoChannelTaskExecutor,
        dataChannelTaskExecutor);
  }


  @TestConfiguration
  static class KafkaChatHomeServiceTestConfiguration
  {
    @Bean
    WorkAssignor infoChannelWorkAssignor()
    {
      return consumer ->
      {
        List<TopicPartition> partitions = consumer
            .partitionsFor(INFO_TOPIC)
            .stream()
            .map(partitionInfo -> new TopicPartition(INFO_TOPIC, partitionInfo.partition()))
            .toList();
        consumer.assign(partitions);
      };
    }
  }
}
