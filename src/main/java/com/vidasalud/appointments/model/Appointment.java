package com.vidasalud.appointments.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "atenciones")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pacienteNombre;
    private Long servicioId;
    private Long boxId;

    @Enumerated(EnumType.STRING)
    private EstadoAtencion estado = EstadoAtencion.SOLICITADA;

    private Instant creadaEn = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPacienteNombre() { return pacienteNombre; }
    public void setPacienteNombre(String v) { this.pacienteNombre = v; }
    public Long getServicioId() { return servicioId; }
    public void setServicioId(Long v) { this.servicioId = v; }
    public Long getBoxId() { return boxId; }
    public void setBoxId(Long v) { this.boxId = v; }
    public EstadoAtencion getEstado() { return estado; }
    public void setEstado(EstadoAtencion v) { this.estado = v; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant v) { this.creadaEn = v; }
}
