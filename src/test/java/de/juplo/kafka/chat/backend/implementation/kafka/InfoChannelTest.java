package de.juplo.kafka.chat.backend.implementation.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.juplo.kafka.chat.backend.ChatBackendProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static de.juplo.kafka.chat.backend.implementation.kafka.InfoChannelTest.*;
import static org.assertj.core.api.Assertions.assertThat;


@SpringJUnitConfig(classes = {
    KafkaAutoConfiguration.class,
    TaskExecutionAutoConfiguration.class,
    KafkaServicesConfiguration.class,
    InfoChannelTest.InfoChannelTestConfiguration.class
})
@EnableConfigurationProperties(ChatBackendProperties.class)
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
public class InfoChannelTest
{
  final static String INFO_TOPIC = "INFO_CHANNEL_TEST_INFO";
  final static String DATA_TOPIC = "INFO_CHANNEL_TEST_DATA";
  final static int NUM_SHARDS = 10;


  @Autowired
  InfoChannel infoChannel;


  @Test
  @DisplayName("The loading completes, if the topic is empty")
  void testLoadingCompletesIfTopicEmpty()
  {
    Awaitility
        .await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> assertThat(infoChannel.getChannelState()).isEqualTo(ChannelState.READY));
  }


  static class InfoChannelTestConfiguration
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

    @Bean
    WorkAssignor dataChannelWorkAssignor()
    {
      return consumer -> log.info("No work is assigned to the DataChannel!");
    }

    @Bean
    ObjectMapper objectMapper()
    {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper;
    }

    @Bean
    Clock clock()
    {
      return Clock.systemDefaultZone();
    }
  }
}
