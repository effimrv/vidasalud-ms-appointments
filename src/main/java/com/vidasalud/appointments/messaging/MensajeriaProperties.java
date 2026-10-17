package com.vidasalud.appointments.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Nombres de RabbitMQ leídos desde {@code vidasalud.mensajeria} en application.yml.
 * Si falta la routing key de algún evento, el servicio no arranca (falla rápido en vez de perder eventos).
 */
@ConfigurationProperties(prefix = "vidasalud.mensajeria")
public record MensajeriaProperties(Exchanges exchanges, Map<TipoEventoCita, String> routingKeys) {

    public record Exchanges(String citas) {}

    public MensajeriaProperties {
        if (exchanges == null || exchanges.citas() == null || exchanges.citas().isBlank()) {
            throw new IllegalStateException("Falta vidasalud.mensajeria.exchanges.citas en application.yml");
        }
        Map<TipoEventoCita, String> configuradas = routingKeys == null ? Map.of() : routingKeys;
        List<TipoEventoCita> faltantes = Arrays.stream(TipoEventoCita.values())
                .filter(tipo -> configuradas.get(tipo) == null || configuradas.get(tipo).isBlank())
                .toList();
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException("Faltan routing keys en vidasalud.mensajeria.routing-keys para: " + faltantes);
        }
        routingKeys = Map.copyOf(routingKeys);
    }

    /** Exchange topic de citas (validado al arrancar). */
    public @NonNull String exchangeCitas() {
        String citas = exchanges.citas();
        if (citas == null) {
            throw new IllegalStateException("Falta vidasalud.mensajeria.exchanges.citas en application.yml");
        }
        return citas;
    }

    public @NonNull String routingKey(TipoEventoCita tipo) {
        String routingKey = routingKeys.get(tipo);
        if (routingKey == null) {
            throw new IllegalStateException("No hay routing key configurada para " + tipo);
        }
        return routingKey;
    }
}
