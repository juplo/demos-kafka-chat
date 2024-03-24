package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static de.juplo.kafka.chat.backend.KafkaApplicationStartupIT.DATA_TOPIC;
import static de.juplo.kafka.chat.backend.KafkaApplicationStartupIT.INFO_TOPIC;


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
class KafkaApplicationStartupIT extends AbstractApplicationStartupIT
{
  final static String INFO_TOPIC = "KAFKA_APPLICATION_STARTUP_IT_INFO_CHANNEL";
  final static String DATA_TOPIC = "KAFKA_APPLICATION_STARTUP_IT_DATA_CHANNEL";
}
