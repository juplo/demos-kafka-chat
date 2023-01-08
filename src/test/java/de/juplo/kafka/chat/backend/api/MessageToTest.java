package de.juplo.kafka.chat.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class MessageToTest
{
  final String json = """
  {
    "id": 1,
    "serial": 0,
    "text": "Hallo, ich heiße Peter!",
    "time": "2023-01-08T00:10:13.625190635",
    "user": "Peter"
  }""";

  ObjectMapper mapper;

  @BeforeEach
  public void setUp()
  {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  public void testDeserialization() throws Exception
  {
    MessageTo message = mapper.readValue(json, MessageTo.class);
    assertThat(message.getId()).isEqualTo(1l);
    assertThat(message.getSerial()).isEqualTo(0l);
    assertThat(message.getText()).isEqualTo("Hallo, ich heiße Peter!");
    assertThat(message.getTime()).isEqualTo("2023-01-08T00:10:13.625190635");
    assertThat(message.getUser()).isEqualTo("Peter");
  }
}
