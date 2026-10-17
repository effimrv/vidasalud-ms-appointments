package com.vidasalud.appointments.controller;

import com.vidasalud.appointments.messaging.CitaEventPublisher;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica que un paciente solo acceda a sus propias atenciones. */
@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AppointmentRepository repo;

    @MockBean
    private CitaEventPublisher eventos;

    @Test
    void conPacienteIdSoloListaLasDeEsePaciente() throws Exception {
        when(repo.findByPacienteId("oid-ana")).thenReturn(List.of(cita(1L, "oid-ana")));

        mvc.perform(get("/api/appointments").param("pacienteId", "oid-ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pacienteId").doesNotExist());
        verify(repo, never()).findAll();
    }

    @Test
    void sinPacienteIdListaTodas() throws Exception {
        when(repo.findAll()).thenReturn(List.of(cita(1L, "oid-ana"), cita(2L, "oid-luis")));

        mvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unaCitaDeOtroPacienteResponde404() throws Exception {
        when(repo.findById(2L)).thenReturn(Optional.of(cita(2L, "oid-luis")));

        mvc.perform(get("/api/appointments/2").param("pacienteId", "oid-ana"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/appointments/2").param("pacienteId", "oid-luis"))
                .andExpect(status().isOk());
    }

    private Appointment cita(Long id, String pacienteId) {
        Appointment a = new Appointment();
        a.setId(id);
        a.setPacienteNombre("Paciente " + id);
        a.setPacienteId(pacienteId);
        a.setServicioId(1L);
        a.setEstado(EstadoAtencion.SOLICITADA);
        return a;
    }
}
