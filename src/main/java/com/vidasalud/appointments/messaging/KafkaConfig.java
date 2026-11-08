package com.vidasalud.appointments.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Declara el tópico de citas al arrancar (KafkaAdmin lo crea si no existe). Las particiones, réplicas
 * y parámetros de retención vienen de application.yml, no están fijos en el código.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicosProperties.class)
public class KafkaConfig {

    @Bean
    public NewTopic topicoCitas(KafkaTopicosProperties props) {
        KafkaTopicosProperties.Topico citas = props.citas();
        return TopicBuilder.name(citas.nombre())
                .partitions(citas.particiones())
                .replicas(citas.replicas())
                .configs(citas.configuracion())
                .build();
    }

    /**
     * Productor con el ObjectMapper de Spring Boot: fechas en ISO-8601 (no números ni arreglos) y sin
     * cabeceras de tipo Java, para que cualquier consumidor lea el mismo contrato JSON.
     */
    @Bean
    public KafkaTemplate<String, CitaEvento> kafkaTemplate(KafkaProperties propiedades, ObjectMapper objectMapper) {
        Map<String, Object> config = propiedades.buildProducerProperties(null);
        config.remove(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
        config.remove(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        // Los ajustes JSON los fija el código (setter); no pueden venir también de las propiedades.
        config.keySet().removeIf(clave -> clave.startsWith("spring.json"));
        // Copia con fechas como texto ISO-8601, sin depender de la configuración global de Jackson.
        ObjectMapper mapper = objectMapper.copy().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonSerializer<CitaEvento> json = new JsonSerializer<>(mapper);
        json.setAddTypeInfo(false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config, new StringSerializer(), json));
    }
}
