package com.vidasalud.appointments.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** El evento de Kafka debe viajar con fechas ISO-8601, no como números ni arreglos. */
class FormatoEventoKafkaTest {

    @Test
    void lasFechasSaleComoTextoIso() {
        // Mismo ajuste que el productor real: fechas como texto ISO-8601.
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        CitaEvento evento = new CitaEvento(TipoEventoCita.CONFIRMADA, 9L, "Ana", "ana@example.com", 1L, 2L,
                "CONFIRMADA", LocalDateTime.of(2026, 11, 10, 10, 30), Instant.parse("2026-11-08T12:00:00Z"), "Sistema");

        String cuerpo;
        try (JsonSerializer<CitaEvento> json = new JsonSerializer<>(mapper)) {
            json.setAddTypeInfo(false);
            cuerpo = new String(json.serialize("t", null, evento), java.nio.charset.StandardCharsets.UTF_8);
        }

        assertThat(cuerpo).contains("\"ocurridoEn\":\"2026-11-08T12:00:00Z\"");
        assertThat(cuerpo).contains("\"fechaHora\":\"2026-11-10T10:30:00\"");
        assertThat(cuerpo).doesNotContain("\"@class\"");
    }
}
