package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Los comodines de Mockito (eq, any) no declaran nulabilidad: se silencia ese aviso del IDE. */
@SuppressWarnings("null")
class KafkaCitaEventPublisherTest {

    private KafkaTemplate<String, CitaEvento> kafkaTemplate;
    private KafkaCitaEventPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString(), any(CitaEvento.class)))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, CitaEvento>) null));
        publisher = new KafkaCitaEventPublisher(kafkaTemplate, new KafkaTopicosProperties(
                new KafkaTopicosProperties.Topicos(new KafkaTopicosProperties.Topico(
                        "appointments.events", 3, (short) 3, Map.of("cleanup.policy", "delete")))));
    }

    @Test
    void publicaEnElTopicoConLaCitaComoClave() {
        publisher.publicarCambio(cita(EstadoAtencion.SOLICITADA), "Paciente");

        verify(kafkaTemplate).send(eq("appointments.events"), eq("10"), any(CitaEvento.class));
    }

    @Test
    void elEventoLlevaElEstadoActualDeLaCita() {
        publisher.publicarCambio(cita(EstadoAtencion.CERRADA), "Paciente");

        verify(kafkaTemplate).send(eq("appointments.events"), eq("10"),
                org.mockito.ArgumentMatchers.argThat(evento -> evento.tipo() == TipoEventoCita.CERRADA
                        && evento.citaId() == 10L));
    }

    @Test
    void siKafkaNoEstaDisponibleNoRompeLaOperacion() {
        doThrow(new KafkaException("sin brokers"))
                .when(kafkaTemplate).send(anyString(), anyString(), any(CitaEvento.class));

        assertThatCode(() -> publisher.publicarCambio(cita(EstadoAtencion.CONFIRMADA), "Paciente")).doesNotThrowAnyException();
    }

    @Test
    void unRechazoAsincronoDelBrokerNoRompeLaOperacion() {
        when(kafkaTemplate.send(anyString(), anyString(), any(CitaEvento.class)))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("NOT_ENOUGH_REPLICAS")));

        assertThatCode(() -> publisher.publicarCambio(cita(EstadoAtencion.EN_ATENCION), "Paciente")).doesNotThrowAnyException();
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
