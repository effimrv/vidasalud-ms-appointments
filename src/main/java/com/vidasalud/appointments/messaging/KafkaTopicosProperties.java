package com.vidasalud.appointments.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Tópicos Kafka leídos desde {@code vidasalud.kafka.topicos} en application.yml.
 * Cada tópico define sus particiones, su factor de replicación y sus parámetros de administración
 * (retención, cleanup policy, mínimo de réplicas sincronizadas).
 */
@ConfigurationProperties(prefix = "vidasalud.kafka")
public record KafkaTopicosProperties(Topicos topicos) {

    public record Topicos(Topico citas) {}

    public record Topico(String nombre, int particiones, short replicas, Map<String, String> configuracion) {}

    public KafkaTopicosProperties {
        if (topicos == null || topicos.citas() == null || topicos.citas().nombre() == null
                || topicos.citas().nombre().isBlank()) {
            throw new IllegalStateException("Falta vidasalud.kafka.topicos.citas.nombre en application.yml");
        }
        validarPositivos(topicos.citas());
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
