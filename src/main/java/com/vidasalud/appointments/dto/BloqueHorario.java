package com.vidasalud.appointments.dto;

/**
 * Hora de la agenda de un día.
 *
 * @param motivo por qué no está disponible (null si está libre)
 */
public record BloqueHorario(String hora, boolean disponible, String motivo) {
}
