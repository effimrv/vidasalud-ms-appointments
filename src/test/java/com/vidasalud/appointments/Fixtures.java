package com.vidasalud.appointments;

import com.vidasalud.appointments.config.FlujoProperties;
import com.vidasalud.appointments.model.EstadoAtencion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** Datos comunes de las pruebas: un reloj fijo (lunes 19-10-2026, 12:00 en Santiago) y la configuración del flujo. */
public final class Fixtures {

    public static final ZoneId ZONA = ZoneId.of("America/Santiago");
    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-19T15:00:00Z"), ZONA);

    private Fixtures() {
    }

    public static FlujoProperties propiedades(int porcentajeBloqueado) {
        return new FlujoProperties(true,
                Map.of(EstadoAtencion.SOLICITADA, Duration.ofSeconds(15), EstadoAtencion.CONFIRMADA, Duration.ofSeconds(25),
                        EstadoAtencion.EN_ESPERA, Duration.ofSeconds(25), EstadoAtencion.EN_ATENCION, Duration.ofSeconds(40)),
                new FlujoProperties.Agenda("America/Santiago", List.of("09:00", "09:30", "15:00", "15:30"), porcentajeBloqueado),
                new FlujoProperties.Demanda(true, 0.6, 0.1, 40, 10));
    }
}
