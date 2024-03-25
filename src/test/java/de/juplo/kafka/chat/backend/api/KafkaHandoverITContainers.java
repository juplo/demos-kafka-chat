package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;


@Slf4j
class KafkaHandoverITContainers extends AbstractHandoverITContainers
{
  @Override
  void setUpExtra() throws IOException, InterruptedException
  {
    kafka.start();

    Container.ExecResult result;
    result = kafka.execInContainer(
        "kafka-topics",
        "--bootstrap-server",
        "kafka:9999",
        "--create",
        "--topic",
        "info_channel",
        "--partitions",
        "3");
    log.info(
        "EXIT-CODE={}, STDOUT={}, STDERR={}",
        result.getExitCode(),
        result.getStdout(),
        result.getStdout());
    result = kafka.execInContainer(
        "kafka-topics",
        "--bootstrap-server",
        "kafka:9999",
        "--create",
        "--topic",
        "data_channel",
        "--partitions",
        "10");
    log.info(
        "EXIT-CODE={}, STDOUT={}, STDERR={}",
        result.getExitCode(),
        result.getStdout(),
        result.getStdout());
  }


  private final KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
          .withNetwork(network)
          .withNetworkAliases("kafka")
          .withListener(() -> "kafka:9999")
          .withKraft()
          .waitingFor(Wait.forLogMessage(".*Kafka\\ Server\\ started.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("KAFKA"));


  @Override
  String[] getBackendCommand()
  {
    return new String[]
    {
        "--chat.backend.instance-id=backend_ID",
        "--chat.backend.services=kafka",
        "--chat.backend.kafka.bootstrap-servers=kafka:9999",
        "--chat.backend.kafka.instance-uri=http://backend-ID:8080",
        "--chat.backend.kafka.num-partitions=10",
        "--chat.backend.kafka.client-id-prefix=BID",
        "--chat.backend.kafka.haproxy-data-plane-api=http://haproxy:5555/v2/",
        "--chat.backend.kafka.haproxy-user=juplo",
        "--chat.backend.kafka.haproxy-password=juplo",
        "--chat.backend.kafka.haproxy-map=sharding",
        "--logging.level.de.juplo=DEBUG"
    };
  }
}
