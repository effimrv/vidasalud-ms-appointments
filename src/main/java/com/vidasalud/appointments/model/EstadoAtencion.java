package com.vidasalud.appointments.model;

import java.util.Set;

public enum EstadoAtencion {
    SOLICITADA, CONFIRMADA, EN_ESPERA, EN_ATENCION, CERRADA, CANCELADA;

    /** Estados en los que la cita sigue avanzando (el flujo automático los procesa). */
    public static final Set<EstadoAtencion> EN_CURSO = Set.of(SOLICITADA, CONFIRMADA, EN_ESPERA, EN_ATENCION);

    /** Transiciones permitidas desde este estado. */
    public boolean puedePasarA(EstadoAtencion nuevo) {
        return switch (this) {
            case SOLICITADA -> nuevo == CONFIRMADA || nuevo == CANCELADA;
            case CONFIRMADA -> nuevo == EN_ESPERA || nuevo == CANCELADA;
            case EN_ESPERA -> nuevo == EN_ATENCION || nuevo == CANCELADA;
            case EN_ATENCION -> nuevo == CERRADA;
            case CERRADA, CANCELADA -> false;
        };
    }
}
