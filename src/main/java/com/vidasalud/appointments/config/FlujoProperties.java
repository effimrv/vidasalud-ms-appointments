package com.vidasalud.appointments.config;

import com.vidasalud.appointments.model.EstadoAtencion;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** Parámetros de la simulación de la operación del centro, leídos desde {@code vidasalud.flujo} en application.yml. */
@ConfigurationProperties(prefix = "vidasalud.flujo")
public record FlujoProperties(boolean automatico, Map<EstadoAtencion, Duration> tiempos, Agenda agenda, Demanda demanda) {

    /**
     * @param bloques             horas de atención ofrecidas cada día (HH:mm)
     * @param porcentajeBloqueado porcentaje de bloques que la agenda del profesional tiene ocupados de antemano
     */
    public record Agenda(String zonaHoraria, List<String> bloques, int porcentajeBloqueado) {
        public ZoneId zona() {
            return ZoneId.of(zonaHoraria);
        }

        public List<LocalTime> horas() {
            return bloques.stream().map(LocalTime::parse).toList();
        }
    }

    /** @param limpiezaTras tiempo tras el cual se eliminan las reservas simuladas finalizadas o canceladas */
    public record Demanda(boolean habilitada, double probabilidadReserva, double probabilidadCancelacion,
                          int maximoReservasFuturas, int diasHorizonte, Duration limpiezaTras) {}

    /** Cuánto permanece una cita en el estado indicado antes de avanzar (por defecto 30 segundos). */
    public Duration tiempoEn(EstadoAtencion estado) {
        return tiempos.getOrDefault(estado, Duration.ofSeconds(30));
    }
}
