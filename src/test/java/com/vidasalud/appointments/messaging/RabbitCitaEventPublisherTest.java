package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitCitaEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private RabbitCitaEventPublisher publisher;

    @BeforeEach
    void preparar() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new RabbitCitaEventPublisher(rabbitTemplate, new MensajeriaProperties("vidasalud.citas.topic", "cita"));
    }

    @Test
    void unaCitaNuevaSePublicaComoCitaCreada() {
        publisher.publicarCambio(cita(EstadoAtencion.SOLICITADA));

        verify(rabbitTemplate).convertAndSend(eq("vidasalud.citas.topic"), eq("cita.creada"), any(CitaEvento.class), any(CorrelationData.class));
    }

    @Test
    void cadaEstadoTieneSuRoutingKey() {
        publisher.publicarCambio(cita(EstadoAtencion.EN_ATENCION));

        verify(rabbitTemplate).convertAndSend(eq("vidasalud.citas.topic"), eq("cita.en_atencion"), any(CitaEvento.class), any(CorrelationData.class));
    }

    @Test
    void siRabbitNoEstaDisponibleNoRompeLaOperacion() {
        doThrow(new AmqpConnectException(new RuntimeException("sin conexión")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class), any(CorrelationData.class));

        assertThatCode(() -> publisher.publicarCambio(cita(EstadoAtencion.CONFIRMADA))).doesNotThrowAnyException();
    }

    private Appointment cita(EstadoAtencion estado) {
        Appointment a = new Appointment();
        a.setId(10L);
        a.setPacienteNombre("Ana");
        a.setServicioId(2L);
        a.setEstado(estado);
        return a;
    }
}
