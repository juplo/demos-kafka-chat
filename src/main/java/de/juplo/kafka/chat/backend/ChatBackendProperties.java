package de.juplo.kafka.chat.backend;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.logging.Level;


@ConfigurationProperties("chat.backend")
@Getter
@Setter
public class ChatBackendProperties
{
  private String instanceId = "DEV";
  private String allowedOrigins = "http://localhost:4200";
  private int chatroomBufferSize = 1024;
  private ServiceType services = ServiceType.inmemory;
  private InMemoryServicesProperties inmemory = new InMemoryServicesProperties();
  private KafkaServicesProperties kafka = new KafkaServicesProperties();
  private ProjectreactorProperties projectreactor = new ProjectreactorProperties();


  @Getter
  @Setter
  public static class InMemoryServicesProperties
  {
    private ShardingStrategyType shardingStrategy = ShardingStrategyType.none;
    private int numShards = 1;
    private int[] ownedShards = new int[0];
    private URI[] shardOwners = new URI[0];
    private StorageStrategyType storageStrategy = StorageStrategyType.none;
    private String storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"),"chat", "backend").toString();
  }

  @Getter
  @Setter
  public static class KafkaServicesProperties
  {
    private URI instanceUri = URI.create("http://localhost:8080");
    private String clientIdPrefix = "DEV";
    private String bootstrapServers = ":9092";
    private String infoChannelTopic = "info_channel";
    private String dataChannelTopic = "data_channel";
    private int numPartitions = 2;
    private Duration pollingInterval = Duration.ofSeconds(1);
    private String haproxyRuntimeApi = "haproxy:8401";
    private String haproxyMap = "/usr/local/etc/haproxy/sharding.map";
  }

  @Getter
  @Setter
  public static class ProjectreactorProperties
  {
    private Level loggingLevel = Level.FINE;
    private boolean showOperatorLine = true;
  }
  public enum ServiceType { inmemory, kafka }
  public enum StorageStrategyType { none, files, mongodb }
  public enum ShardingStrategyType { none, kafkalike }
}
