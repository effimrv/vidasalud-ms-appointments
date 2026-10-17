package com.vidasalud.appointments.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Registro de cada cambio de estado de una atención: qué pasó, cuándo y quién lo hizo. */
@Entity
@Table(name = "historial_atenciones", indexes = @Index(columnList = "atencionId"))
public class CambioEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long atencionId;

    @Enumerated(EnumType.STRING)
    private EstadoAtencion estado;

    private Instant fecha;

    /** Quién originó el cambio: "Paciente", "Sistema · revisión automática", "Recepción · Ana Pérez", etc. */
    private String responsable;

    private String detalle;

    protected CambioEstado() {
    }

    public CambioEstado(Long atencionId, EstadoAtencion estado, Instant fecha, String responsable, String detalle) {
        this.atencionId = atencionId;
        this.estado = estado;
        this.fecha = fecha;
        this.responsable = responsable;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Long getAtencionId() { return atencionId; }
    public EstadoAtencion getEstado() { return estado; }
    public Instant getFecha() { return fecha; }
    public String getResponsable() { return responsable; }
    public String getDetalle() { return detalle; }
}
