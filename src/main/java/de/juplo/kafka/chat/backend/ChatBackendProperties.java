package de.juplo.kafka.chat.backend;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Paths;


@ConfigurationProperties("chat.backend")
@Getter
@Setter
public class ChatBackendProperties
{
  private String allowedOrigins = "http://localhost:4200";
  private int chatroomBufferSize = 8;
  private ServiceType services = ServiceType.inmemory;
  private InMemoryServicesProperties inmemory = new InMemoryServicesProperties();


  @Getter
  @Setter
  public static class InMemoryServicesProperties
  {
    private ShardingStrategyType shardingStrategy = ShardingStrategyType.none;
    private int numShards = 1;
    private int[] ownedShards = new int[] { 0 };
    private StorageStrategyType storageStrategy = StorageStrategyType.none;
    private String storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"),"chat", "backend").toString();
  }

  public enum ServiceType { inmemory }
  public enum StorageStrategyType { none, files, mongodb }
  public enum ShardingStrategyType { none, kafkalike }
}
