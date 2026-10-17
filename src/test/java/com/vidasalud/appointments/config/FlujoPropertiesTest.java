package com.vidasalud.appointments.config;

import com.vidasalud.appointments.model.EstadoAtencion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprueba que application.yml define los tiempos del flujo y la agenda. */
class FlujoPropertiesTest {

    @Test
    void cargaLaConfiguracionDelFlujoDesdeApplicationYml() throws Exception {
        StandardEnvironment entorno = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(entorno.getPropertySources()::addLast);

        FlujoProperties props = new Binder(ConfigurationPropertySources.get(entorno), new PropertySourcesPlaceholdersResolver(entorno))
                .bind("vidasalud.flujo", FlujoProperties.class).get();

        assertThat(props.automatico()).isTrue();
        assertThat(props.tiempoEn(EstadoAtencion.SOLICITADA)).isEqualTo(Duration.ofSeconds(15));
        assertThat(props.tiempoEn(EstadoAtencion.EN_ESPERA)).isEqualTo(Duration.ofSeconds(25));
        assertThat(props.tiempoEn(EstadoAtencion.EN_ATENCION)).isEqualTo(Duration.ofSeconds(40));
        assertThat(props.agenda().horas()).contains(LocalTime.of(9, 0), LocalTime.of(16, 30));
        assertThat(props.agenda().zona().getId()).isEqualTo("America/Santiago");
        assertThat(props.demanda().habilitada()).isTrue();
        assertThat(props.demanda().maximoReservasFuturas()).isEqualTo(12);
        assertThat(props.demanda().limpiezaTras()).isEqualTo(Duration.ofMinutes(15));
    }
}
