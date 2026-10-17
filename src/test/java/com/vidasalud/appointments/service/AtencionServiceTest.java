package com.vidasalud.appointments.service;

import com.vidasalud.appointments.Fixtures;
import com.vidasalud.appointments.messaging.CitaEventPublisher;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.CambioEstado;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import com.vidasalud.appointments.repository.CambioEstadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Mockito, AssertJ, MockMvc y los repositorios de Spring Data no declaran nulabilidad: se silencia ese aviso del IDE. */
@SuppressWarnings("null")
class AtencionServiceTest {

    private final AppointmentRepository repo = mock(AppointmentRepository.class);
    private final CambioEstadoRepository historial = mock(CambioEstadoRepository.class);
    private final AgendaService agenda = mock(AgendaService.class);
    private final CitaEventPublisher eventos = mock(CitaEventPublisher.class);
    private AtencionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AtencionService(repo, historial, agenda, eventos, Fixtures.RELOJ);
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            if (a.getId() == null) a.setId(10L);
            return a;
        });
    }

    @Test
    void creaLaSolicitudConHistorialYEvento() {
        when(agenda.motivoNoDisponible(any(), any())).thenReturn(Optional.empty());

        Appointment creada = servicio.crear(nueva(), AtencionService.RESPONSABLE_PACIENTE);

        assertThat(creada.getEstado()).isEqualTo(EstadoAtencion.SOLICITADA);
        assertThat(creada.getActualizadaEn()).isEqualTo(Fixtures.RELOJ.instant());
        ArgumentCaptor<CambioEstado> registro = ArgumentCaptor.forClass(CambioEstado.class);
        verify(historial).save(registro.capture());
        assertThat(registro.getValue().getResponsable()).isEqualTo("Paciente");
        verify(eventos).publicarCambio(creada);
    }

    @Test
    void rechazaUnaHoraQueYaNoEstaDisponible() {
        when(agenda.motivoNoDisponible(any(), any())).thenReturn(Optional.of(AgendaService.MOTIVO_RESERVADA));

        assertThatThrownBy(() -> servicio.crear(nueva(), "Paciente"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(repo, never()).save(any());
    }

    @Test
    void unPacienteSoloPuedeCancelarSusCitas() {
        Appointment propia = nueva();
        propia.setId(5L);
        propia.setPacienteId("oid-ana");
        propia.setEstado(EstadoAtencion.CONFIRMADA);
        when(repo.findById(5L)).thenReturn(Optional.of(propia));

        assertThatThrownBy(() -> servicio.cambiarEstado(5L, EstadoAtencion.EN_ESPERA, "oid-ana", "Paciente", "x"))
                .hasMessageContaining("403");
        assertThatThrownBy(() -> servicio.cambiarEstado(5L, EstadoAtencion.CANCELADA, "oid-otro", "Paciente", "x"))
                .hasMessageContaining("404");

        Appointment cancelada = servicio.cambiarEstado(5L, EstadoAtencion.CANCELADA, "oid-ana", "Paciente", "Cancelada");
        assertThat(cancelada.getEstado()).isEqualTo(EstadoAtencion.CANCELADA);
        assertThat(cancelada.getObservacion()).isEqualTo("Cancelada");
    }

    @Test
    void noPermiteSaltarseEstados() {
        Appointment cita = nueva();
        cita.setEstado(EstadoAtencion.SOLICITADA);

        assertThatThrownBy(() -> servicio.aplicar(cita, EstadoAtencion.CERRADA, "Recepción", "x"))
                .hasMessageContaining("400");
    }

    private Appointment nueva() {
        Appointment a = new Appointment();
        a.setPacienteNombre("Ana Pérez");
        a.setServicioId(1L);
        a.setFechaHora(LocalDateTime.of(2026, 10, 20, 9, 0));
        return a;
    }
}
