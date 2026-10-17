package com.vidasalud.appointments.controller;

import com.vidasalud.appointments.dto.BloqueHorario;
import com.vidasalud.appointments.dto.DetalleAtencion;
import com.vidasalud.appointments.model.Appointment;
import com.vidasalud.appointments.model.EstadoAtencion;
import com.vidasalud.appointments.service.AgendaService;
import com.vidasalud.appointments.service.AtencionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mockito, AssertJ, MockMvc y los repositorios de Spring Data no declaran nulabilidad: se silencia ese aviso del IDE. */
@SuppressWarnings("null")
@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AtencionService atenciones;

    @MockBean
    private AgendaService agenda;

    @Test
    void listaSoloLasDelPacienteSinExponerSuIdentificador() throws Exception {
        when(atenciones.listar(null, "oid-ana")).thenReturn(List.of(cita(1L, "oid-ana")));

        mvc.perform(get("/api/appointments").param("pacienteId", "oid-ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pacienteId").doesNotExist());
    }

    @Test
    void elDetalleIncluyeElHistorial() throws Exception {
        when(atenciones.detalle(1L, null)).thenReturn(new DetalleAtencion(cita(1L, "oid-ana"), List.of()));

        mvc.perform(get("/api/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.historial").isArray());
    }

    @Test
    void devuelveLaDisponibilidadDelDia() throws Exception {
        when(agenda.disponibilidad(2L, LocalDate.of(2026, 10, 20)))
                .thenReturn(List.of(new BloqueHorario("09:00", false, AgendaService.MOTIVO_RESERVADA)));

        mvc.perform(get("/api/appointments/disponibilidad").param("servicioId", "2").param("fecha", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponible").value(false))
                .andExpect(jsonPath("$[0].motivo").value(AgendaService.MOTIVO_RESERVADA));
    }

    @Test
    void cancelarUsaElResponsableEnviadoPorElBff() throws Exception {
        when(atenciones.cambiarEstado(eq(3L), eq(EstadoAtencion.CANCELADA), eq(null), eq("Recepción · Ara"), any()))
                .thenReturn(cita(3L, "oid-luis"));

        mvc.perform(put("/api/appointments/3/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELADA\",\"responsable\":\"Recepción · Ara\"}"))
                .andExpect(status().isOk());
        verify(atenciones).cambiarEstado(eq(3L), eq(EstadoAtencion.CANCELADA), eq(null), eq("Recepción · Ara"),
                eq("Cita cancelada por Recepción · Ara."));
    }

    @Test
    void rechazaUnEstadoInexistente() throws Exception {
        mvc.perform(put("/api/appointments/3/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VOLANDO\"}"))
                .andExpect(status().isBadRequest());
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
