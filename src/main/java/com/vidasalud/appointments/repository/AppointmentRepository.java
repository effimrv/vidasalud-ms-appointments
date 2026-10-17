package com.vidasalud.appointments.repository;

import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByEstado(EstadoAtencion estado);
    List<Appointment> findByPacienteId(String pacienteId);
    List<Appointment> findByPacienteIdAndEstado(String pacienteId, EstadoAtencion estado);
}
