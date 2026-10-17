package com.vidasalud.appointments.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprueba que application.yml define el exchange y la routing key de cada evento. */
class MensajeriaPropertiesTest {

    @Test
    void cargaLasRoutingKeysDesdeApplicationYml() throws Exception {
        List<PropertySource<?>> fuentes = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        StandardEnvironment entorno = new StandardEnvironment();
        fuentes.forEach(entorno.getPropertySources()::addLast);

        MensajeriaProperties props = new Binder(ConfigurationPropertySources.get(entorno))
                .bind("vidasalud.mensajeria", MensajeriaProperties.class).get();

        assertThat(props.exchangeCitas()).isEqualTo("vidasalud.citas.topic");
        assertThat(props.routingKey(TipoEventoCita.CREADA)).isEqualTo("cita.creada");
        assertThat(props.routingKey(TipoEventoCita.EN_ESPERA)).isEqualTo("cita.en_espera");
        assertThat(props.routingKey(TipoEventoCita.EN_ATENCION)).isEqualTo("cita.en_atencion");
        assertThat(props.routingKey(TipoEventoCita.CERRADA)).isEqualTo("cita.cerrada");
    }
}
