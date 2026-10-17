package com.vidasalud.appointments.dto;

/**
 * Cambio de estado manual (cancelación por paciente o recepción).
 *
 * @param responsable quién lo hace; lo completa el BFF a partir del token
 */
public record CambioEstadoRequest(String status, String responsable, String detalle) {
}
