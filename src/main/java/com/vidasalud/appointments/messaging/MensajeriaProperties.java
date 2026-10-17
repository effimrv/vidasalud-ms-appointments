package com.vidasalud.appointments.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Nombres de RabbitMQ leídos desde {@code vidasalud.mensajeria} en application.yml. */
@ConfigurationProperties(prefix = "vidasalud.mensajeria")
public record MensajeriaProperties(String exchangeCitas, String prefijoRoutingKey) {
}
