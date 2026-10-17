package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;

import java.time.Instant;
import java.time.LocalDateTime;

/** Mensaje que se publica en RabbitMQ cuando una cita se crea o cambia de estado. */
public record CitaEvento(
        TipoEventoCita tipo,
        Long citaId,
        String pacienteNombre,
        Long servicioId,
        Long boxId,
        String estado,
        LocalDateTime fechaHora,
        Instant ocurridoEn) {

    public static CitaEvento desde(Appointment cita) {
        return new CitaEvento(TipoEventoCita.desde(cita.getEstado()), cita.getId(), cita.getPacienteNombre(),
                cita.getServicioId(), cita.getBoxId(), cita.getEstado().name(), cita.getFechaHora(), Instant.now());
    }
}
