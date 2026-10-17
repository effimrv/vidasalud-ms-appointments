package com.vidasalud.appointments.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería del productor. Las colas y sus DLQ las declara ms-notify (el consumidor);
 * aquí solo se declara el exchange donde se publica, para que exista aunque ms-notify aún no haya arrancado.
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    /** Exchange topic de eventos de citas (caso de uso: notificar, emitir tickets y generar comprobantes). */
    @Bean
    public TopicExchange exchangeCitas(MensajeriaProperties props) {
        return ExchangeBuilder.topicExchange(props.exchanges().citas()).durable(true).build();
    }

    /** Serializa los eventos como JSON con fechas ISO-8601 (usa el ObjectMapper de Spring Boot). */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** Registra en el log la confirmación del broker y los mensajes que no llegaron a ninguna cola. */
    @Bean
    public RabbitTemplateCustomizer confirmacionesDelBroker() {
        return template -> {
            template.setMandatory(true);
            template.setConfirmCallback((correlacion, ack, causa) -> {
                String id = correlacion == null ? "?" : correlacion.getId();
                if (ack) {
                    log.debug("Broker confirmó el evento {}", id);
                } else {
                    log.error("Broker rechazó el evento {}: {}", id, causa);
                }
            });
            template.setReturnsCallback(devuelto -> log.warn(
                    "Evento sin cola de destino: exchange={} routingKey={} ({})",
                    devuelto.getExchange(), devuelto.getRoutingKey(), devuelto.getReplyText()));
        };
    }
}
