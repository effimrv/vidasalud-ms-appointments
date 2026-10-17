package com.vidasalud.appointments.repository;

import com.vidasalud.appointments.model.CambioEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {
    List<CambioEstado> findByAtencionIdOrderByFechaAscIdAsc(Long atencionId);
}
