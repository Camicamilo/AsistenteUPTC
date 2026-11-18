package co.edu.uptc.asistente.infraestructura.entrada.rest;

import co.edu.uptc.asistente.aplicacion.dto.NuevaSesion;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarSesiones;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.ProcesarConsulta;
import co.edu.uptc.asistente.dominio.modelo.RespuestaChat;
import co.edu.uptc.asistente.dominio.modelo.TemaRapido;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.ConsultaPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.NuevaSesionPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.OkDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Adaptador de entrada del chat del estudiante (IF-01). Público y anónimo. */
@Tag(name = "Chat", description = "Widget del estudiante (sesiones anónimas)")
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ProcesarConsulta procesarConsulta;
    private final GestionarSesiones sesiones;

    public ChatController(ProcesarConsulta procesarConsulta, GestionarSesiones sesiones) {
        this.procesarConsulta = procesarConsulta;
        this.sesiones = sesiones;
    }

    @Operation(summary = "Inicia una sesión anónima (UC-04)")
    @PostMapping("/sesiones")
    @ResponseStatus(HttpStatus.CREATED)
    public NuevaSesion iniciar(@RequestBody(required = false) NuevaSesionPeticion peticion) {
        return sesiones.iniciar(peticion == null ? null : peticion.canal());
    }

    @Operation(summary = "Envía una consulta: devuelve respuesta o escalamiento (UC-05, RF-01 a RF-05)")
    @PostMapping("/consultas")
    public RespuestaChat consultar(@RequestBody ConsultaPeticion peticion) {
        return procesarConsulta.procesar(peticion.sesionId(), peticion.texto());
    }

    @Operation(summary = "Cierra la sesión (UC-09)")
    @PostMapping("/sesiones/{sesionId}/cerrar")
    public OkDto cerrar(@PathVariable String sesionId) {
        sesiones.cerrar(sesionId);
        return new OkDto(true);
    }

    @Operation(summary = "Temas de acceso rápido del widget")
    @GetMapping("/temas")
    public List<TemaRapido> temas() {
        return sesiones.temas();
    }
}
