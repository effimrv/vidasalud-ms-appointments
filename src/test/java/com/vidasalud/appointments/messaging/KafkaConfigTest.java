package com.vidasalud.appointments.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigTest {

    @Test
    void elProductorSeCreaConPropiedadesPorDefecto() {
        KafkaTemplate<String, CitaEvento> template = new KafkaConfig()
                .kafkaTemplate(new KafkaProperties(), new ObjectMapper());

        assertThat(template).isNotNull();
    }
}
