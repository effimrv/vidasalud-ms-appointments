package com.vidasalud.appointments.flujo;

import com.vidasalud.appointments.config.FlujoProperties;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import com.vidasalud.appointments.service.AtencionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Avanza solas las citas en curso: cada una pasa al siguiente estado cuando cumple el tiempo configurado
 * (vidasalud.flujo.tiempos). Simula, de forma acelerada, la operación real de revisión, admisión y atención.
 */
@Component
public class FlujoAutomatico {

    private static final Logger log = LoggerFactory.getLogger(FlujoAutomatico.class);

    private final AppointmentRepository repo;
    private final AtencionService atenciones;
    private final FlujoProperties props;
    private final Clock reloj;

    public FlujoAutomatico(AppointmentRepository repo, AtencionService atenciones, FlujoProperties props, Clock reloj) {
        this.repo = repo;
        this.atenciones = atenciones;
        this.props = props;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${vidasalud.flujo.intervalo-ms:5000}", initialDelay = 10_000)
    public void avanzarCitas() {
        if (!props.automatico()) {
            return;
        }
        Instant ahora = Instant.now(reloj);
        for (Appointment atencion : repo.findByEstadoIn(EstadoAtencion.EN_CURSO)) {
            try {
                avanzarSiCorresponde(atencion, ahora);
            } catch (RuntimeException e) {
                log.warn("No se pudo avanzar la atención {}: {}", atencion.getId(), e.getMessage());
            }
        }
    }

    private void avanzarSiCorresponde(Appointment atencion, Instant ahora) {
        if (atencion.getActualizadaEn() == null) {
            // Citas creadas antes del flujo automático: empiezan a contar desde ahora.
            atencion.setActualizadaEn(ahora);
            repo.save(atencion);
            return;
        }
        Duration enEstado = Duration.between(atencion.getActualizadaEn(), ahora);
        if (enEstado.compareTo(props.tiempoEn(atencion.getEstado())) < 0) {
            return;
        }
        PasoAutomatico.desde(atencion.getEstado()).ifPresent(paso -> {
            atenciones.aplicar(atencion, paso.hacia, paso.responsable, paso.detallePara(atencion));
            log.info("Atención {}: {} -> {} ({})", atencion.getId(), paso.desde, paso.hacia, paso.responsable);
        });
    }
}
