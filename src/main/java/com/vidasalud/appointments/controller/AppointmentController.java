package com.vidasalud.appointments.controller;

import com.vidasalud.appointments.messaging.CitaEventPublisher;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository repo;
    private final CitaEventPublisher eventos;

    public AppointmentController(@NonNull AppointmentRepository repo, @NonNull CitaEventPublisher eventos) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.eventos = Objects.requireNonNull(eventos, "eventos must not be null");
    }

    /** Lista las atenciones. Con pacienteId (lo envía el BFF para los pacientes) devuelve solo las de ese paciente. */
    @GetMapping
    public List<Appointment> list(@RequestParam(required = false) EstadoAtencion status,
                                  @RequestParam(required = false) String pacienteId) {
        if (pacienteId != null) {
            return (status == null) ? repo.findByPacienteId(pacienteId) : repo.findByPacienteIdAndEstado(pacienteId, status);
        }
        return (status == null) ? repo.findAll() : repo.findByEstado(status);
    }

    /** Con pacienteId, una atención de otro paciente se responde como inexistente (404). */
    @GetMapping("/{id}")
    public Appointment getById(@PathVariable("id") @NonNull Long id,
                               @RequestParam(required = false) String pacienteId) {
        return repo.findById(id)
            .filter(a -> pacienteId == null || pacienteId.equals(a.getPacienteId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atencion no encontrada"));
    }

    @PostMapping
    public Appointment create(@RequestBody @NonNull Appointment a) {
        a.setId(null);
        a.setEstado(EstadoAtencion.SOLICITADA);
        Appointment guardada = repo.save(a);
        eventos.publicarCambio(guardada);
        return guardada;
    }

    @PutMapping("/{id}/status")
    public Appointment changeStatus(@PathVariable("id") @NonNull Long id, @RequestBody @NonNull Map<String, String> body) {
        Appointment a = getById(id, null);
        EstadoAtencion nuevo = EstadoAtencion.valueOf(Objects.requireNonNull(body.get("status"), "status must not be null"));
        validarTransicion(a.getEstado(), nuevo);
        a.setEstado(nuevo);
        Appointment guardada = repo.save(a);
        eventos.publicarCambio(guardada);
        return guardada;
    }

    private void validarTransicion(EstadoAtencion actual, EstadoAtencion nuevo) {
        boolean valido = switch (actual) {
            case SOLICITADA -> nuevo == EstadoAtencion.CONFIRMADA || nuevo == EstadoAtencion.CANCELADA;
            case CONFIRMADA -> nuevo == EstadoAtencion.EN_ESPERA || nuevo == EstadoAtencion.CANCELADA;
            case EN_ESPERA  -> nuevo == EstadoAtencion.EN_ATENCION || nuevo == EstadoAtencion.CANCELADA;
            case EN_ATENCION -> nuevo == EstadoAtencion.CERRADA;
            default -> false;
        };
        if (!valido) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Transicion no permitida: " + actual + " -> " + nuevo);
        }
    }
}
