package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.EstadoAtencion;

/** Eventos del ciclo de vida de una cita. El nombre en minúsculas forma la routing key (cita.confirmada, ...). */
public enum TipoEventoCita {
    CREADA, CONFIRMADA, EN_ESPERA, EN_ATENCION, CERRADA, CANCELADA;

    public static TipoEventoCita desde(EstadoAtencion estado) {
        return estado == EstadoAtencion.SOLICITADA ? CREADA : valueOf(estado.name());
    }

    public String segmentoRoutingKey() {
        return name().toLowerCase();
    }
}
