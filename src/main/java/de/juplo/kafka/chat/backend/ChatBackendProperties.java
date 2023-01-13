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
    private ShardingStrategyType shardingStrategy = ShardingStrategyType.kafkalike;
    private int numShards = 10;
    private int[] ownedShards = { 2 };
    private StorageStrategyType storageStrategy = StorageStrategyType.files;
    private String storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"),"chat", "backend").toString();
  }

  public enum ServiceType { inmemory }
  public enum StorageStrategyType { files, mongodb }
  public enum ShardingStrategyType { none, kafkalike }
}
