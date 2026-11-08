package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Los comodines de Mockito (any) no declaran nulabilidad: se silencia ese aviso del IDE. */
@SuppressWarnings("null")
class PublicadorCitasCompuestoTest {

    @Test
    void envianElEventoATodosLosDestinos() {
        RabbitCitaEventPublisher rabbit = mock(RabbitCitaEventPublisher.class);
        KafkaCitaEventPublisher kafka = mock(KafkaCitaEventPublisher.class);
        Appointment cita = cita();

        new PublicadorCitasCompuesto(rabbit, kafka).publicarCambio(cita);

        verify(rabbit).publicarCambio(cita);
        verify(kafka).publicarCambio(cita);
    }

    @Test
    void unFalloEnRabbitNoImpideEnviarASKafka() {
        RabbitCitaEventPublisher rabbit = mock(RabbitCitaEventPublisher.class);
        KafkaCitaEventPublisher kafka = mock(KafkaCitaEventPublisher.class);
        doThrow(new IllegalStateException("rabbit caído")).when(rabbit).publicarCambio(any());
        Appointment cita = cita();

        assertThatCode(() -> new PublicadorCitasCompuesto(rabbit, kafka).publicarCambio(cita)).doesNotThrowAnyException();
        verify(kafka).publicarCambio(cita);
    }

    private Appointment cita() {
        Appointment a = new Appointment();
        a.setId(7L);
        a.setEstado(EstadoAtencion.CONFIRMADA);
        return a;
    }
}
