package com.vidasalud.appointments.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Tópicos Kafka leídos desde {@code vidasalud.kafka.topicos} en application.yml.
 * Cada tópico define sus particiones, su factor de replicación y sus parámetros de administración
 * (retención, cleanup policy, mínimo de réplicas sincronizadas).
 */
@ConfigurationProperties(prefix = "vidasalud.kafka")
public record KafkaTopicosProperties(Topicos topicos) {

    public record Topicos(Topico citas) {}

    public record Topico(@NonNull String nombre, int particiones, short replicas,
                         @NonNull Map<String, String> configuracion) {}

    public KafkaTopicosProperties {
        Topico citas = topicos == null ? null : topicos.citas();
        if (citas == null || citas.nombre().isBlank()) {
            throw new IllegalStateException("Falta vidasalud.kafka.topicos.citas.nombre en application.yml");
        }
        validarPositivos(citas);
    }

    private static void validarPositivos(Topico topico) {
        if (topico.particiones() < 1 || topico.replicas() < 1) {
            throw new IllegalStateException("El tópico " + topico.nombre() + " necesita particiones y réplicas mayores que 0");
        }
    }

    /** Tópico de eventos de citas: lo publica ms-appointments y lo consumen ms-report y ms-audit. */
    public Topico citas() {
        return topicos.citas();
    }
}
