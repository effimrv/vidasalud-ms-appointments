package com.vidasalud.appointments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "atenciones")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pacienteNombre;
    /**
     * Identificador del paciente en Entra ID (claim oid del token), lo asigna el BFF.
     * Solo se recibe, no se devuelve en las respuestas.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pacienteId;
    private Long servicioId;
    private Long boxId;

    @Enumerated(EnumType.STRING)
    private EstadoAtencion estado = EstadoAtencion.SOLICITADA;

    private Instant creadaEn = Instant.now();

    /** Fecha y hora para la que el paciente agendó la atención (distinto de creadaEn, que es cuándo se solicitó). */
    private LocalDateTime fechaHora;

    /** Correo del paciente (lo asigna el BFF desde el token), visible para el personal del centro. */
    private String pacienteCorreo;

    /** Último cambio de estado: el flujo automático mide desde aquí cuánto lleva la cita en su estado actual. */
    private Instant actualizadaEn;

    /** Explicación del último cambio de estado, visible para el paciente y la recepción. */
    private String observacion;

    /** true si la reserva la generó la demanda simulada (paciente ficticio de demostración). */
    private Boolean simulada;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPacienteNombre() { return pacienteNombre; }
    public void setPacienteNombre(String v) { this.pacienteNombre = v; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String v) { this.pacienteId = v; }
    public Long getServicioId() { return servicioId; }
    public void setServicioId(Long v) { this.servicioId = v; }
    public Long getBoxId() { return boxId; }
    public void setBoxId(Long v) { this.boxId = v; }
    public EstadoAtencion getEstado() { return estado; }
    public void setEstado(EstadoAtencion v) { this.estado = v; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant v) { this.creadaEn = v; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime v) { this.fechaHora = v; }
    public String getPacienteCorreo() { return pacienteCorreo; }
    public void setPacienteCorreo(String v) { this.pacienteCorreo = v; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(Instant v) { this.actualizadaEn = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { this.observacion = v; }
    public boolean isSimulada() { return Boolean.TRUE.equals(simulada); }
    public void setSimulada(boolean v) { this.simulada = v; }
}
