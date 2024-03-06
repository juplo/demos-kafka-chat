package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.implementation.kafka.ChannelTaskExecutor;
import de.juplo.kafka.chat.backend.implementation.kafka.DataChannel;
import de.juplo.kafka.chat.backend.implementation.kafka.KafkaTestUtils;
import de.juplo.kafka.chat.backend.implementation.kafka.WorkAssignor;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

  @BeforeAll
  static void sendAndLoadStoredData(
      @Autowired KafkaTemplate<String, String> messageTemplate,
      @Autowired ChannelTaskExecutor dataChannelTaskExecutor)
  {
    KafkaTestUtils.sendAndLoadStoredData(
        messageTemplate,
        INFO_TOPIC,
        DATA_TOPIC);

    // The initialization of the data-channel must happen,
    // after the messages were sent into the topic of the
    // test-cluster.
    // Otherwise, the initial loading of the data might be
    // completed, before these messages arrive, so that
    // they are ignored and the state is never restored.
    dataChannelTaskExecutor.executeChannelTask();
  }


  @TestConfiguration
  @Import(KafkaTestUtils.KafkaTestConfiguration.class)
  static class KafkaConfigurationITConfiguration
  {
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
}
