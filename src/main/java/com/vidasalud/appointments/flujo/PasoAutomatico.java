package com.vidasalud.appointments.flujo;

import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;

import java.util.Optional;

/** Lo que hace el sistema cuando una cita cumple su tiempo en cada estado. */
enum PasoAutomatico {

    REVISION(EstadoAtencion.SOLICITADA, EstadoAtencion.CONFIRMADA, "Sistema · revisión automática",
            "Solicitud revisada: hora disponible y prestación vigente. Cita confirmada."),
    ADMISION(EstadoAtencion.CONFIRMADA, EstadoAtencion.EN_ESPERA, "Sistema · admisión",
            "El paciente registró su llegada y espera ser llamado."),
    LLAMADO(EstadoAtencion.EN_ESPERA, EstadoAtencion.EN_ATENCION, "Sistema · llamado a box",
            "Paciente llamado a su box de atención."),
    CIERRE(EstadoAtencion.EN_ATENCION, EstadoAtencion.CERRADA, "Sistema · cierre de atención",
            "Atención finalizada. Informe médico y comprobante disponibles.");

    final EstadoAtencion desde;
    final EstadoAtencion hacia;
    final String responsable;
    private final String detalle;

    PasoAutomatico(EstadoAtencion desde, EstadoAtencion hacia, String responsable, String detalle) {
        this.desde = desde;
        this.hacia = hacia;
        this.responsable = responsable;
        this.detalle = detalle;
    }

    String detallePara(Appointment atencion) {
        return (this == LLAMADO && atencion.getBoxId() != null)
                ? "Paciente llamado al Box " + atencion.getBoxId() + "."
                : detalle;
    }

    static Optional<PasoAutomatico> desde(EstadoAtencion estado) {
        for (PasoAutomatico paso : values()) {
            if (paso.desde == estado) return Optional.of(paso);
        }
        return Optional.empty();
    }
}
