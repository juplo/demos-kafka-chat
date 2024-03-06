package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.implementation.kafka.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

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
      @Autowired ChannelTaskRunner channelTaskRunner)
  {
    KafkaTestUtils.sendAndLoadStoredData(
        messageTemplate,
        INFO_TOPIC,
        DATA_TOPIC,
        channelTaskRunner);
  }

  @AfterAll
  static void joinChannels(
      @Autowired ChannelTaskExecutor dataChannelTaskExecutor,
      @Autowired ChannelTaskExecutor infoChannelTaskExecutor)
  {
    dataChannelTaskExecutor.join();
    infoChannelTaskExecutor.join();
  }


  @TestConfiguration
  @Import(KafkaTestUtils.KafkaTestConfiguration.class)
  static class KafkaConfigurationITConfiguration
  {
  }
}
