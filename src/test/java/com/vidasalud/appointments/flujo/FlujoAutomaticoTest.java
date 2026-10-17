package com.vidasalud.appointments.flujo;

import com.vidasalud.appointments.Fixtures;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import com.vidasalud.appointments.service.AtencionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlujoAutomaticoTest {

    private final AppointmentRepository repo = mock(AppointmentRepository.class);
    private final AtencionService atenciones = mock(AtencionService.class);
    private final FlujoAutomatico flujo = new FlujoAutomatico(repo, atenciones, Fixtures.propiedades(0), Fixtures.RELOJ);

    @Test
    void confirmaUnaSolicitudQueCumplioSuTiempoDeRevision() {
        Appointment cita = cita(EstadoAtencion.SOLICITADA, 16);
        when(repo.findByEstadoIn(EstadoAtencion.EN_CURSO)).thenReturn(List.of(cita));

        flujo.avanzarCitas();

        verify(atenciones).aplicar(eq(cita), eq(EstadoAtencion.CONFIRMADA), eq("Sistema · revisión automática"), any());
    }

    @Test
    void esperaSiLaCitaAunNoCumpleSuTiempo() {
        Appointment cita = cita(EstadoAtencion.EN_ATENCION, 20);
        when(repo.findByEstadoIn(EstadoAtencion.EN_CURSO)).thenReturn(List.of(cita));

        flujo.avanzarCitas();

        verify(atenciones, never()).aplicar(any(), any(), any(), any());
    }

    @Test
    void elLlamadoIndicaElBoxAsignado() {
        Appointment cita = cita(EstadoAtencion.EN_ESPERA, 30);
        cita.setBoxId(4L);
        when(repo.findByEstadoIn(EstadoAtencion.EN_CURSO)).thenReturn(List.of(cita));

        flujo.avanzarCitas();

        verify(atenciones).aplicar(cita, EstadoAtencion.EN_ATENCION, "Sistema · llamado a box", "Paciente llamado al Box 4.");
    }

    private Appointment cita(EstadoAtencion estado, long segundosEnEstado) {
        Appointment a = new Appointment();
        a.setId(1L);
        a.setEstado(estado);
        a.setActualizadaEn(Fixtures.RELOJ.instant().minusSeconds(segundosEnEstado));
        return a;
    }
}
