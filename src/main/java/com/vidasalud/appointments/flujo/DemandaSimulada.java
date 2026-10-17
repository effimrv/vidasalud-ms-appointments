package com.vidasalud.appointments.flujo;

import com.vidasalud.appointments.config.FlujoProperties;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import com.vidasalud.appointments.service.AgendaService;
import com.vidasalud.appointments.service.AtencionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simula la demanda de otros pacientes: cada cierto tiempo alguien reserva una hora libre (y a veces cancela),
 * así la agenda se va ocupando como en un centro real. Las reservas quedan marcadas como simuladas.
 */
@Component
public class DemandaSimulada {

    private static final Logger log = LoggerFactory.getLogger(DemandaSimulada.class);
    private static final String RESPONSABLE = "Paciente · reserva en línea";
    private static final String DOMINIO_CORREO = "@demo.vidasalud.cl";
    private static final List<Long> SERVICIOS_POR_DEFECTO = List.of(1L, 2L, 3L, 4L, 5L, 6L);
    private static final int INTENTOS_POR_RESERVA = 10;

    private static final List<String> NOMBRES = List.of("Camila", "Benjamín", "Valentina", "Matías", "Isidora",
            "Agustín", "Josefa", "Tomás", "Martina", "Vicente", "Florencia", "Joaquín", "Antonella", "Cristóbal",
            "Catalina", "Lucas", "Fernanda", "Maximiliano", "Javiera", "Sebastián");
    private static final List<String> APELLIDOS = List.of("González", "Muñoz", "Rojas", "Díaz", "Pérez", "Soto",
            "Contreras", "Silva", "Martínez", "Sepúlveda", "Morales", "Rodríguez", "López", "Fuentes", "Hernández",
            "Torres", "Araya", "Flores", "Espinoza", "Valenzuela");

    private final AppointmentRepository repo;
    private final AtencionService atenciones;
    private final AgendaService agenda;
    private final FlujoProperties props;
    private final Clock reloj;

    public DemandaSimulada(AppointmentRepository repo, AtencionService atenciones, AgendaService agenda,
                           FlujoProperties props, Clock reloj) {
        this.repo = repo;
        this.atenciones = atenciones;
        this.agenda = agenda;
        this.props = props;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${vidasalud.flujo.demanda.intervalo-ms:45000}", initialDelay = 20_000)
    public void simular() {
        FlujoProperties.Demanda demanda = props.demanda();
        if (!demanda.habilitada()) {
            return;
        }
        ThreadLocalRandom azar = ThreadLocalRandom.current();
        try {
            if (azar.nextDouble() < demanda.probabilidadCancelacion()) {
                cancelarUnaReserva(azar);
            }
            if (azar.nextDouble() < demanda.probabilidadReserva()) {
                reservarUnaHora(azar);
            }
        } catch (RuntimeException e) {
            log.warn("La demanda simulada no pudo completar su ciclo: {}", e.getMessage());
        }
    }

    private void reservarUnaHora(ThreadLocalRandom azar) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        long futuras = repo.countBySimuladaTrueAndFechaHoraAfterAndEstadoNot(ahora, EstadoAtencion.CANCELADA);
        if (futuras >= props.demanda().maximoReservasFuturas()) {
            return;
        }
        List<Long> servicios = repo.findServiciosReservados();
        if (servicios.isEmpty()) {
            servicios = SERVICIOS_POR_DEFECTO;
        }
        List<LocalTime> bloques = agenda.bloques();
        for (int intento = 0; intento < INTENTOS_POR_RESERVA; intento++) {
            Long servicioId = servicios.get(azar.nextInt(servicios.size()));
            LocalDate dia = ahora.toLocalDate().plusDays(1 + azar.nextInt(props.demanda().diasHorizonte()));
            if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            LocalDateTime fechaHora = dia.atTime(bloques.get(azar.nextInt(bloques.size())));
            if (agenda.motivoNoDisponible(servicioId, fechaHora).isEmpty()) {
                Appointment reserva = pacienteSimulado(azar, servicioId, fechaHora);
                Appointment creada = atenciones.crear(reserva, RESPONSABLE);
                log.info("Demanda simulada: {} reservó la atención {} para {}", creada.getPacienteNombre(), creada.getId(), fechaHora);
                return;
            }
        }
    }

    private void cancelarUnaReserva(ThreadLocalRandom azar) {
        List<Appointment> cancelables = repo.findBySimuladaTrueAndEstadoIn(
                Set.of(EstadoAtencion.SOLICITADA, EstadoAtencion.CONFIRMADA));
        if (cancelables.isEmpty()) {
            return;
        }
        Appointment elegida = cancelables.get(azar.nextInt(cancelables.size()));
        atenciones.aplicar(elegida, EstadoAtencion.CANCELADA, RESPONSABLE,
                "El paciente canceló su reserva; la hora quedó disponible.");
        log.info("Demanda simulada: se canceló la atención {}", elegida.getId());
    }

    private Appointment pacienteSimulado(ThreadLocalRandom azar, Long servicioId, LocalDateTime fechaHora) {
        String nombre = NOMBRES.get(azar.nextInt(NOMBRES.size()));
        String apellido = APELLIDOS.get(azar.nextInt(APELLIDOS.size()));
        Appointment reserva = new Appointment();
        reserva.setPacienteNombre(nombre + " " + apellido);
        reserva.setPacienteId("simulado-" + UUID.randomUUID());
        reserva.setPacienteCorreo(sinTildes(nombre + "." + apellido).toLowerCase() + DOMINIO_CORREO);
        reserva.setServicioId(servicioId);
        reserva.setBoxId(servicioId);
        reserva.setFechaHora(fechaHora);
        reserva.setSimulada(true);
        return reserva;
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
