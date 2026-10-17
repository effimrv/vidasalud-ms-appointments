package com.vidasalud.appointments.repository;

import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByEstado(EstadoAtencion estado);
    List<Appointment> findByPacienteId(String pacienteId);
    List<Appointment> findByPacienteIdAndEstado(String pacienteId, EstadoAtencion estado);
    List<Appointment> findByEstadoIn(Collection<EstadoAtencion> estados);

    /** Reservas que ocupan horas de una prestación en un rango (las canceladas liberan la hora). */
    List<Appointment> findByServicioIdAndFechaHoraBetweenAndEstadoNot(Long servicioId, LocalDateTime desde,
                                                                     LocalDateTime hasta, EstadoAtencion excluido);

    boolean existsByServicioIdAndFechaHoraAndEstadoNot(Long servicioId, LocalDateTime fechaHora, EstadoAtencion excluido);

    long countBySimuladaTrueAndFechaHoraAfterAndEstadoNot(LocalDateTime desde, EstadoAtencion excluido);

    List<Appointment> findBySimuladaTrueAndEstadoIn(Collection<EstadoAtencion> estados);

    @Query("select distinct a.servicioId from Appointment a where a.servicioId is not null")
    List<Long> findServiciosReservados();
}
