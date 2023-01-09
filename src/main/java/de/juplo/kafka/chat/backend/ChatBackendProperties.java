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
  private String storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"),"chat", "backend").toString();
  private String allowedOrigins = "http://localhost:4200";
  private int chatroomBufferSize = 8;
}
