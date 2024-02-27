package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;


@Slf4j
class KafkaHandoverIT extends AbstractHandoverIT
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


  KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
          .withNetwork(network)
          .withNetworkAliases("kafka")
          .withListener(() -> "kafka:9999")
          .withKraft()
          .waitingFor(Wait.forLogMessage(".*Kafka\\ Server\\ started.*\\n", 1))
          .withLogConsumer(new Slf4jLogConsumer(log, true).withPrefix("KAFKA"));

  @Override
  String[] getCommandBackend1()
  {
    return new String[]
    {
        "--chat.backend.instance-id=backend_1",
        "--chat.backend.services=kafka",
        "--chat.backend.kafka.bootstrap-servers=kafka:9999",
        "--chat.backend.kafka.instance-uri=http://backend-1:8080",
        "--chat.backend.kafka.num-partitions=10",
        "--chat.backend.kafka.client-id-prefix=B1",
        "--chat.backend.kafka.haproxy-runtime-api=haproxy:8401",
        "--chat.backend.kafka.haproxy-map=/usr/local/etc/haproxy/sharding.map"
    };
  }

  @Override
  String[] getCommandBackend2()
  {
    return new String[]
    {
        "--chat.backend.instance-id=backend_2",
        "--chat.backend.services=kafka",
        "--chat.backend.kafka.bootstrap-servers=kafka:9999",
        "--chat.backend.kafka.instance-uri=http://backend-2:8080",
        "--chat.backend.kafka.num-partitions=10",
        "--chat.backend.kafka.client-id-prefix=B2",
        "--chat.backend.kafka.haproxy-runtime-api=haproxy:8401",
        "--chat.backend.kafka.haproxy-map=/usr/local/etc/haproxy/sharding.map"
    };
  }

  @Override
  String[] getCommandBackend3()
  {
    return new String[]
    {
        "--chat.backend.instance-id=backend_3",
        "--chat.backend.services=kafka",
        "--chat.backend.kafka.bootstrap-servers=kafka:9999",
        "--chat.backend.kafka.instance-uri=http://backend-3:8080",
        "--chat.backend.kafka.num-partitions=10",
        "--chat.backend.kafka.client-id-prefix=B3",
        "--chat.backend.kafka.haproxy-runtime-api=haproxy:8401",
        "--chat.backend.kafka.haproxy-map=/usr/local/etc/haproxy/sharding.map"
    };
  }
}
