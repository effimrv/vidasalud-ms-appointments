package com.vidasalud.appointments.messaging;

import com.vidasalud.appointments.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Productor Kafka de los eventos de citas. La clave del mensaje es el id de la cita: todos los eventos
 * de una misma cita caen en la misma partición y se consumen en orden.
 * <p>
 * El envío es asíncrono: el resultado (partición y offset, o el error) se registra en el log cuando el
 * broker responde. Si Kafka no está disponible, la cita se guarda igual y el error queda en el log.
 */
@Component
public class KafkaCitaEventPublisher implements CitaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaCitaEventPublisher.class);

    private final KafkaTemplate<String, CitaEvento> kafkaTemplate;
    private final KafkaTopicosProperties props;

    public KafkaCitaEventPublisher(KafkaTemplate<String, CitaEvento> kafkaTemplate, KafkaTopicosProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    @Override
    public void publicarCambio(Appointment cita) {
        CitaEvento evento = CitaEvento.desde(cita);
        String topico = props.citas().nombre();
        String clave = claveDe(cita);
        try {
            kafkaTemplate.send(topico, clave, evento).whenComplete((resultado, error) -> registrar(topico, clave, evento, resultado, error));
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el evento {} de la cita {} a Kafka ({}); la cita se guardó igual. Causa: {}",
                    evento.tipo(), cita.getId(), topico, e.getMessage());
        }
    }

    /** Clave del mensaje: el id de la cita como texto. */
    static @NonNull String claveDe(Appointment cita) {
        return String.valueOf(cita.getId());
    }

    private void registrar(String topico, String clave, CitaEvento evento,
                           SendResult<String, CitaEvento> resultado, Throwable error) {
        if (error != null) {
            log.error("Kafka rechazó el evento {} de la cita {} en {}: {}", evento.tipo(), clave, topico, error.getMessage());
        } else if (resultado != null && resultado.getRecordMetadata() != null) {
            log.info("Evento publicado en Kafka: {} partición={} offset={} (cita {}, {})", topico,
                    resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset(), clave, evento.tipo());
        }
    }
}
