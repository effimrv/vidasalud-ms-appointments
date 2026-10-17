package com.vidasalud.appointments.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.CambioEstado;

import java.util.List;

/** Una atención con todos sus datos más el historial de cambios de estado, en orden cronológico. */
public record DetalleAtencion(@JsonUnwrapped Appointment atencion, List<CambioEstado> historial) {
}
