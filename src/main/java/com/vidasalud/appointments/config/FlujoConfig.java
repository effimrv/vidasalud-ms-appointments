package com.vidasalud.appointments.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/** Activa las tareas programadas del flujo automático y fija el reloj en la zona horaria del centro. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(FlujoProperties.class)
public class FlujoConfig {

    @Bean
    public Clock reloj(FlujoProperties props) {
        return Clock.system(props.agenda().zona());
    }
}
