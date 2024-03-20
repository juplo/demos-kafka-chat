package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;


@Slf4j
class KafkaHandoverIT extends AbstractHandoverIT
{
  KafkaHandoverIT()
  {
    super(new KafkaHandoverITContainers());
  }
}
