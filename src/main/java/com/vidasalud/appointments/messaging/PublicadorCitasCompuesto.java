package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Publicador principal de eventos de citas: envía cada cambio a RabbitMQ (notificaciones, tickets y
 * comprobantes) y a Kafka (auditoría y reportería). Cada destino es independiente: si uno falla,
 * el otro igual recibe el evento y la operación HTTP no se interrumpe.
 */
@Primary
@Component
public class PublicadorCitasCompuesto implements CitaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PublicadorCitasCompuesto.class);

    private final RabbitCitaEventPublisher rabbit;
    private final KafkaCitaEventPublisher kafka;

    public PublicadorCitasCompuesto(RabbitCitaEventPublisher rabbit, KafkaCitaEventPublisher kafka) {
        this.rabbit = rabbit;
        this.kafka = kafka;
    }

    @Override
    public void publicarCambio(Appointment cita, String responsable) {
        publicarEn("RabbitMQ", () -> rabbit.publicarCambio(cita, responsable));
        publicarEn("Kafka", () -> kafka.publicarCambio(cita, responsable));
    }

    private void publicarEn(String destino, Runnable publicacion) {
        try {
            publicacion.run();
        } catch (RuntimeException e) {
            log.error("Falló la publicación en {}: {}", destino, e.getMessage());
        }
    }
}
