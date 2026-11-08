package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublicadorCitasCompuestoTest {

    @Test
    void envianElEventoATodosLosDestinos() {
        RabbitCitaEventPublisher rabbit = mock(RabbitCitaEventPublisher.class);
        KafkaCitaEventPublisher kafka = mock(KafkaCitaEventPublisher.class);
        Appointment cita = cita();

        new PublicadorCitasCompuesto(rabbit, kafka).publicarCambio(cita, "Paciente");

        verify(rabbit).publicarCambio(cita, "Paciente");
        verify(kafka).publicarCambio(cita, "Paciente");
    }

    @Test
    void unFalloEnRabbitNoImpideEnviarASKafka() {
        RabbitCitaEventPublisher rabbit = mock(RabbitCitaEventPublisher.class);
        KafkaCitaEventPublisher kafka = mock(KafkaCitaEventPublisher.class);
        doThrow(new IllegalStateException("rabbit caído")).when(rabbit).publicarCambio(any(), any());
        Appointment cita = cita();

        assertThatCode(() -> new PublicadorCitasCompuesto(rabbit, kafka).publicarCambio(cita, "Paciente")).doesNotThrowAnyException();
        verify(kafka).publicarCambio(cita, "Paciente");
    }

    private Appointment cita() {
        Appointment a = new Appointment();
        a.setId(7L);
        a.setEstado(EstadoAtencion.CONFIRMADA);
        return a;
    }
}
