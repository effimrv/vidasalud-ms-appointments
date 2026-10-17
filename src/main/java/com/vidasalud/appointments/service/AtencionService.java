package com.vidasalud.appointments.service;

import com.vidasalud.appointments.dto.DetalleAtencion;
import com.vidasalud.appointments.messaging.CitaEventPublisher;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.CambioEstado;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import com.vidasalud.appointments.repository.CambioEstadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Reglas de negocio de las atenciones: creación con validación de agenda, cambios de estado con transiciones
 * controladas, historial de cada cambio y publicación del evento correspondiente en RabbitMQ.
 */
@Service
public class AtencionService {

    public static final String RESPONSABLE_PACIENTE = "Paciente";

    private final AppointmentRepository repo;
    private final CambioEstadoRepository historial;
    private final AgendaService agenda;
    private final CitaEventPublisher eventos;
    private final Clock reloj;

    public AtencionService(AppointmentRepository repo, CambioEstadoRepository historial, AgendaService agenda,
                           CitaEventPublisher eventos, Clock reloj) {
        this.repo = repo;
        this.historial = historial;
        this.agenda = agenda;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<Appointment> listar(EstadoAtencion estado, String pacienteId) {
        if (pacienteId != null) {
            return (estado == null) ? repo.findByPacienteId(pacienteId) : repo.findByPacienteIdAndEstado(pacienteId, estado);
        }
        return (estado == null) ? repo.findAll() : repo.findByEstado(estado);
    }

    /** Con pacienteId, una atención de otro paciente se responde como inexistente (404). */
    @Transactional(readOnly = true)
    public Appointment obtener(@NonNull Long id, String pacienteId) {
        return repo.findById(id)
                .filter(a -> pacienteId == null || pacienteId.equals(a.getPacienteId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atención no encontrada"));
    }

    @Transactional(readOnly = true)
    public DetalleAtencion detalle(@NonNull Long id, String pacienteId) {
        Appointment atencion = obtener(id, pacienteId);
        return new DetalleAtencion(atencion, historial.findByAtencionIdOrderByFechaAscIdAsc(atencion.getId()));
    }

    /** Registra una solicitud nueva si la hora sigue disponible (409 si otra persona la tomó antes). */
    @Transactional
    public Appointment crear(Appointment datos, String responsable) {
        if (datos.getServicioId() == null || datos.getFechaHora() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar la prestación y la fecha y hora de la cita");
        }
        agenda.motivoNoDisponible(datos.getServicioId(), datos.getFechaHora()).ifPresent(motivo -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La hora seleccionada no está disponible: " + motivo);
        });

        Instant ahora = Instant.now(reloj);
        datos.setId(null);
        datos.setEstado(EstadoAtencion.SOLICITADA);
        datos.setCreadaEn(ahora);
        datos.setActualizadaEn(ahora);
        datos.setObservacion("Solicitud recibida. En revisión automática.");
        Appointment guardada = repo.save(datos);
        historial.save(new CambioEstado(guardada.getId(), EstadoAtencion.SOLICITADA, ahora, responsable,
                "Solicitud de hora registrada en línea."));
        eventos.publicarCambio(guardada);
        return guardada;
    }

    /**
     * Cambio de estado manual. Con pacienteId (lo envía el BFF para los pacientes) solo se permite cancelar
     * una atención propia.
     */
    @Transactional
    public Appointment cambiarEstado(@NonNull Long id, EstadoAtencion nuevo, String pacienteId, String responsable, String detalle) {
        Appointment atencion = obtener(id, pacienteId);
        if (pacienteId != null && nuevo != EstadoAtencion.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un paciente solo puede cancelar sus citas");
        }
        return aplicar(atencion, nuevo, responsable, detalle);
    }

    /** Aplica una transición validada, la deja en el historial y publica el evento. */
    @Transactional
    public Appointment aplicar(Appointment atencion, EstadoAtencion nuevo, String responsable, String detalle) {
        if (!atencion.getEstado().puedePasarA(nuevo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transición no permitida: " + atencion.getEstado() + " -> " + nuevo);
        }
        Instant ahora = Instant.now(reloj);
        atencion.setEstado(nuevo);
        atencion.setActualizadaEn(ahora);
        atencion.setObservacion(detalle);
        Appointment guardada = repo.save(atencion);
        historial.save(new CambioEstado(guardada.getId(), nuevo, ahora, responsable, detalle));
        eventos.publicarCambio(guardada);
        return guardada;
    }
}
