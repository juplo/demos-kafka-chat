package de.juplo.kafka.chat.backend;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
@EnableConfigurationProperties(ChatBackendProperties.class)
public class ChatBackendConfiguration
{
  @Bean
  Clock clock()
  {
    return Clock.systemDefaultZone();
  }


  @ConditionalOnExpression("!'${chat.backend.inmemory.storage-strategy}'.toLowerCase().equals('mongodb')")
  @Configuration
  @EnableAutoConfiguration(exclude = {
      MongoReactiveDataAutoConfiguration.class,
      MongoReactiveAutoConfiguration.class,
      MongoReactiveRepositoriesAutoConfiguration.class })
  public static class DisableMongoDBConfiguration
  {
  }
}
