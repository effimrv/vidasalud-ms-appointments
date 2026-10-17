package com.vidasalud.appointments.service;

import com.vidasalud.appointments.config.FlujoProperties;
import com.vidasalud.appointments.dto.BloqueHorario;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

/**
 * Disponibilidad de horas por prestación y día. Una hora no está disponible si ya está reservada, si ya pasó,
 * si es domingo o si la agenda del profesional la tiene bloqueada (un porcentaje que varía por día y especialidad).
 */
@Service
public class AgendaService {

    public static final String MOTIVO_RESERVADA = "Hora ya reservada";
    public static final String MOTIVO_BLOQUEADA = "Agenda del profesional no disponible";
    public static final String MOTIVO_PASADA = "Horario ya transcurrido";
    public static final String MOTIVO_DOMINGO = "Sin atención los domingos";
    public static final String MOTIVO_FUERA_DE_AGENDA = "Hora fuera de la agenda del centro";

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository repo;
    private final FlujoProperties props;
    private final Clock reloj;

    public AgendaService(AppointmentRepository repo, FlujoProperties props, Clock reloj) {
        this.repo = repo;
        this.props = props;
        this.reloj = reloj;
    }

    public List<LocalTime> bloques() {
        return props.agenda().horas();
    }

    public List<BloqueHorario> disponibilidad(Long servicioId, LocalDate fecha) {
        Set<LocalTime> reservadas = repo.findByServicioIdAndFechaHoraBetweenAndEstadoNot(servicioId,
                        fecha.atStartOfDay(), fecha.atTime(LocalTime.MAX), EstadoAtencion.CANCELADA).stream()
                .map(Appointment::getFechaHora)
                .map(LocalDateTime::toLocalTime)
                .collect(Collectors.toSet());
        return bloques().stream()
                .map(hora -> {
                    String motivo = reservadas.contains(hora)
                            ? MOTIVO_RESERVADA
                            : motivoSinReservas(servicioId, fecha.atTime(hora)).orElse(null);
                    return new BloqueHorario(hora.format(FORMATO_HORA), motivo == null, motivo);
                })
                .toList();
    }

    /** Vacío si la hora se puede reservar; si no, el motivo. */
    public Optional<String> motivoNoDisponible(Long servicioId, LocalDateTime fechaHora) {
        if (repo.existsByServicioIdAndFechaHoraAndEstadoNot(servicioId, fechaHora, EstadoAtencion.CANCELADA)) {
            return Optional.of(MOTIVO_RESERVADA);
        }
        return motivoSinReservas(servicioId, fechaHora);
    }

    private Optional<String> motivoSinReservas(Long servicioId, LocalDateTime fechaHora) {
        if (!bloques().contains(fechaHora.toLocalTime())) {
            return Optional.of(MOTIVO_FUERA_DE_AGENDA);
        }
        if (fechaHora.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return Optional.of(MOTIVO_DOMINGO);
        }
        if (!fechaHora.isAfter(LocalDateTime.now(reloj))) {
            return Optional.of(MOTIVO_PASADA);
        }
        if (bloqueadaPorAgenda(servicioId, fechaHora)) {
            return Optional.of(MOTIVO_BLOQUEADA);
        }
        return Optional.empty();
    }

    /** Pseudoaleatorio pero estable: la misma hora de la misma especialidad y día siempre da el mismo resultado. */
    private boolean bloqueadaPorAgenda(Long servicioId, LocalDateTime fechaHora) {
        long semilla = servicioId * 1_000_003L + fechaHora.toLocalDate().toEpochDay() * 10_007L
                + fechaHora.toLocalTime().toSecondOfDay();
        return new SplittableRandom(semilla).nextInt(100) < props.agenda().porcentajeBloqueado();
    }
}
