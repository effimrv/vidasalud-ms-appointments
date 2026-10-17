package com.vidasalud.appointments.controller;

import com.vidasalud.appointments.dto.BloqueHorario;
import com.vidasalud.appointments.dto.CambioEstadoRequest;
import com.vidasalud.appointments.dto.DetalleAtencion;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.service.AgendaService;
import com.vidasalud.appointments.service.AtencionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AtencionService atenciones;
    private final AgendaService agenda;

    public AppointmentController(AtencionService atenciones, AgendaService agenda) {
        this.atenciones = atenciones;
        this.agenda = agenda;
    }

    /** Lista las atenciones. Con pacienteId (lo envía el BFF para los pacientes) devuelve solo las de ese paciente. */
    @GetMapping
    public List<Appointment> list(@RequestParam(required = false) EstadoAtencion status,
                                  @RequestParam(required = false) String pacienteId) {
        return atenciones.listar(status, pacienteId);
    }

    /** Horas de un día para una prestación, indicando cuáles están ocupadas y por qué. */
    @GetMapping("/disponibilidad")
    public List<BloqueHorario> disponibilidad(@RequestParam Long servicioId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return agenda.disponibilidad(servicioId, fecha);
    }

    /** Detalle con historial. Con pacienteId, una atención de otro paciente se responde como inexistente (404). */
    @GetMapping("/{id}")
    public DetalleAtencion getById(@PathVariable("id") @NonNull Long id,
                                   @RequestParam(required = false) String pacienteId) {
        return atenciones.detalle(id, pacienteId);
    }

    @PostMapping
    public Appointment create(@RequestBody @NonNull Appointment a) {
        return atenciones.crear(a, AtencionService.RESPONSABLE_PACIENTE);
    }

    @PutMapping("/{id}/status")
    public Appointment changeStatus(@PathVariable("id") @NonNull Long id,
                                    @RequestParam(required = false) String pacienteId,
                                    @RequestBody @NonNull CambioEstadoRequest cambio) {
        EstadoAtencion nuevo;
        try {
            nuevo = EstadoAtencion.valueOf(cambio.status());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido: " + cambio.status());
        }
        String responsable = (cambio.responsable() == null || cambio.responsable().isBlank())
                ? "Recepción" : cambio.responsable();
        String detalle = (cambio.detalle() == null || cambio.detalle().isBlank())
                ? (nuevo == EstadoAtencion.CANCELADA ? "Cita cancelada por " + responsable + "." : "Estado actualizado por " + responsable + ".")
                : cambio.detalle();
        return atenciones.cambiarEstado(id, nuevo, pacienteId, responsable, detalle);
    }
}
