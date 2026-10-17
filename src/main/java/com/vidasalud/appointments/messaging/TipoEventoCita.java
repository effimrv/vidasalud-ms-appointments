package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.EstadoAtencion;

/** Eventos del ciclo de vida de una cita. Cada uno tiene su routing key en application.yml. */
public enum TipoEventoCita {
    CREADA, CONFIRMADA, EN_ESPERA, EN_ATENCION, CERRADA, CANCELADA;

    public static TipoEventoCita desde(EstadoAtencion estado) {
        return estado == EstadoAtencion.SOLICITADA ? CREADA : valueOf(estado.name());
    }
}
