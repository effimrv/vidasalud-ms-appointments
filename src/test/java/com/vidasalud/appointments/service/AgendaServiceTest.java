package com.vidasalud.appointments.service;

import com.vidasalud.appointments.Fixtures;
import com.vidasalud.appointments.dto.BloqueHorario;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgendaServiceTest {

    private final AppointmentRepository repo = mock(AppointmentRepository.class);

    @Test
    void marcaComoOcupadaUnaHoraYaReservada() {
        AgendaService agenda = new AgendaService(repo, Fixtures.propiedades(0), Fixtures.RELOJ);
        LocalDate martes = LocalDate.of(2026, 10, 20);
        Appointment reservada = new Appointment();
        reservada.setFechaHora(martes.atTime(9, 30));
        when(repo.findByServicioIdAndFechaHoraBetweenAndEstadoNot(eq(1L), any(), any(), eq(EstadoAtencion.CANCELADA)))
                .thenReturn(List.of(reservada));

        List<BloqueHorario> bloques = agenda.disponibilidad(1L, martes);

        assertThat(bloques).extracting(BloqueHorario::hora).containsExactly("09:00", "09:30", "15:00", "15:30");
        assertThat(bloques.get(1).disponible()).isFalse();
        assertThat(bloques.get(1).motivo()).isEqualTo(AgendaService.MOTIVO_RESERVADA);
        assertThat(bloques.get(0).disponible()).isTrue();
    }

    @Test
    void lasHorasQueYaPasaronYLosDomingosNoEstanDisponibles() {
        AgendaService agenda = new AgendaService(repo, Fixtures.propiedades(0), Fixtures.RELOJ);

        assertThat(agenda.motivoNoDisponible(1L, LocalDateTime.of(2026, 10, 19, 9, 0)))
                .contains(AgendaService.MOTIVO_PASADA);
        assertThat(agenda.motivoNoDisponible(1L, LocalDateTime.of(2026, 10, 19, 15, 0))).isEmpty();
        assertThat(agenda.motivoNoDisponible(1L, LocalDateTime.of(2026, 10, 25, 9, 0)))
                .contains(AgendaService.MOTIVO_DOMINGO);
        assertThat(agenda.motivoNoDisponible(1L, LocalDateTime.of(2026, 10, 20, 12, 0)))
                .contains(AgendaService.MOTIVO_FUERA_DE_AGENDA);
    }

    @Test
    void elBloqueoDeAgendaEsEstableYDependeDelPorcentaje() {
        LocalDate dia = LocalDate.of(2026, 10, 21);
        assertThat(new AgendaService(repo, Fixtures.propiedades(100), Fixtures.RELOJ).disponibilidad(3L, dia))
                .allSatisfy(b -> assertThat(b.motivo()).isEqualTo(AgendaService.MOTIVO_BLOQUEADA));

        AgendaService agenda = new AgendaService(repo, Fixtures.propiedades(50), Fixtures.RELOJ);
        assertThat(agenda.disponibilidad(3L, dia)).isEqualTo(agenda.disponibilidad(3L, dia));
    }
}
