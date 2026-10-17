package com.vidasalud.appointments.repository;

import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.CambioEstado;
import com.vidasalud.appointments.model.EstadoAtencion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Valida el mapeo de las entidades y las consultas de los repositorios contra una base H2 en memoria. */
/** Mockito, AssertJ, MockMvc y los repositorios de Spring Data no declaran nulabilidad: se silencia ese aviso del IDE. */
@SuppressWarnings("null")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class AppointmentRepositoryTest {

    private static final LocalDateTime MARTES_9 = LocalDateTime.of(2026, 10, 20, 9, 0);

    @Autowired
    private AppointmentRepository repo;

    @Autowired
    private CambioEstadoRepository historial;

    @Test
    void lasConsultasDeAgendaYDemandaFuncionan() {
        repo.save(cita(1L, MARTES_9, EstadoAtencion.CONFIRMADA, true));
        repo.save(cita(1L, MARTES_9.plusMinutes(30), EstadoAtencion.CANCELADA, true));
        repo.save(cita(2L, MARTES_9, EstadoAtencion.SOLICITADA, false));

        assertThat(repo.existsByServicioIdAndFechaHoraAndEstadoNot(1L, MARTES_9, EstadoAtencion.CANCELADA)).isTrue();
        assertThat(repo.existsByServicioIdAndFechaHoraAndEstadoNot(1L, MARTES_9.plusMinutes(30), EstadoAtencion.CANCELADA)).isFalse();
        assertThat(repo.findByServicioIdAndFechaHoraBetweenAndEstadoNot(1L, MARTES_9.minusHours(1), MARTES_9.plusHours(8),
                EstadoAtencion.CANCELADA)).hasSize(1);
        assertThat(repo.countBySimuladaTrueAndFechaHoraAfterAndEstadoNot(MARTES_9.minusDays(1), EstadoAtencion.CANCELADA)).isEqualTo(1);
        assertThat(repo.findBySimuladaTrueAndEstadoIn(Set.of(EstadoAtencion.CONFIRMADA))).hasSize(1);
        assertThat(repo.findByEstadoIn(EstadoAtencion.EN_CURSO)).hasSize(2);
        assertThat(repo.findServiciosReservados()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void elHistorialSeLeeEnOrdenCronologico() {
        Instant t = Instant.parse("2026-10-19T15:00:00Z");
        historial.save(new CambioEstado(7L, EstadoAtencion.CONFIRMADA, t.plusSeconds(15), "Sistema", "b"));
        historial.save(new CambioEstado(7L, EstadoAtencion.SOLICITADA, t, "Paciente", "a"));

        assertThat(historial.findByAtencionIdOrderByFechaAscIdAsc(7L))
                .extracting(CambioEstado::getEstado)
                .containsExactly(EstadoAtencion.SOLICITADA, EstadoAtencion.CONFIRMADA);
    }

    private Appointment cita(Long servicioId, LocalDateTime fechaHora, EstadoAtencion estado, boolean simulada) {
        Appointment a = new Appointment();
        a.setPacienteNombre("Paciente");
        a.setServicioId(servicioId);
        a.setFechaHora(fechaHora);
        a.setEstado(estado);
        a.setSimulada(simulada);
        return a;
    }
}
