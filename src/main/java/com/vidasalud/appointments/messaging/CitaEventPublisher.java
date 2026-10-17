package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;

/** Punto de salida de los eventos de citas. El controlador depende de esta interfaz, no de RabbitMQ. */
public interface CitaEventPublisher {

    /** Publica el evento correspondiente al estado actual de la cita (creada, confirmada, cerrada, ...). */
    void publicarCambio(Appointment cita);
}
