package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de citas en el exchange topic. Si RabbitMQ no está disponible solo se registra
 * el error: la cita ya quedó guardada y la mensajería no debe romper la lógica existente.
 */
@Component
public class RabbitCitaEventPublisher implements CitaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitCitaEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public RabbitCitaEventPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @Override
    public void publicarCambio(Appointment cita) {
        CitaEvento evento = CitaEvento.desde(cita);
        String routingKey = props.routingKey(evento.tipo());
        try {
            rabbitTemplate.convertAndSend(props.exchanges().citas(), routingKey, evento,
                    new CorrelationData("cita-" + cita.getId() + "-" + evento.tipo()));
            log.info("Evento publicado: {} → {} (cita {})", props.exchanges().citas(), routingKey, cita.getId());
        } catch (AmqpException e) {
            log.error("No se pudo publicar {} de la cita {}; la cita se guardó igual. Causa: {}",
                    routingKey, cita.getId(), e.getMessage());
        }
    }
}
