package co.edu.uptc.asistente.infraestructura.entrada.rest;

import co.edu.uptc.asistente.aplicacion.dto.NuevaIntencion;
import co.edu.uptc.asistente.aplicacion.dto.Pagina;
import co.edu.uptc.asistente.aplicacion.dto.ResumenIntencion;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarConocimiento;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.EditarIntencionPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.EditarRespuestaPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.NuevaIntencionPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.TextoPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.EventoDto;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.IntencionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Base de conocimiento desde el panel (UC-12, UC-13) — KnowledgeBaseManager. */
@Tag(name = "Base de conocimiento", description = "Intenciones y respuestas (UC-12, UC-13)")
@RestController
@RequestMapping("/api/admin")
public class IntencionesController {
    private final GestionarConocimiento conocimiento;
    private final ObjectMapper json;

    public IntencionesController(GestionarConocimiento conocimiento, ObjectMapper json) {
        this.conocimiento = conocimiento;
        this.json = json;
    }

    @Operation(summary = "Lista paginada con búsqueda y filtro por categoría")
    @GetMapping("/intenciones")
    public Pagina<ResumenIntencion> listar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String categoria,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "8") int pageSize) {
        return conocimiento.listar(q, categoria, page, pageSize);
    }

    @GetMapping("/intenciones/{id}")
    public IntencionDto obtener(@PathVariable long id) {
        return IntencionDto.de(conocimiento.obtener(id));
    }

    @Operation(summary = "Crea una intención con su primera respuesta y sus ejemplos (reentrena el PLN)")
    @PostMapping("/intenciones")
    @ResponseStatus(HttpStatus.CREATED)
    public IntencionDto crear(@AuthenticationPrincipal Usuario actor, @RequestBody NuevaIntencionPeticion p) {
        return IntencionDto.de(conocimiento.crear(new NuevaIntencion(p.nombre(), p.categoria(), p.descripcion(),
                p.dependencia(), p.respuesta(), p.ejemplos()), actor));
    }

    @PutMapping("/intenciones/{id}")
    public IntencionDto actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable long id,
                                   @RequestBody EditarIntencionPeticion p) {
        return IntencionDto.de(conocimiento.actualizar(id, p.categoria(), p.descripcion(), p.dependencia(), actor));
    }

    @Operation(summary = "Agrega una respuesta alternativa")
    @PostMapping("/intenciones/{id}/respuestas")
    public IntencionDto agregarRespuesta(@AuthenticationPrincipal Usuario actor, @PathVariable long id,
                                         @RequestBody TextoPeticion p) {
        return IntencionDto.de(conocimiento.agregarRespuesta(id, p.texto(), actor));
    }

    @Operation(summary = "Edita, activa o desactiva una respuesta (nunca se borra)")
    @PatchMapping("/intenciones/{id}/respuestas/{respuestaId}")
    public IntencionDto actualizarRespuesta(@AuthenticationPrincipal Usuario actor, @PathVariable long id,
                                            @PathVariable long respuestaId, @RequestBody EditarRespuestaPeticion p) {
        return IntencionDto.de(conocimiento.actualizarRespuesta(id, respuestaId, p.texto(), p.activa(), actor));
    }

    @Operation(summary = "Historial de cambios leído del almacén de eventos (EventSourcing)")
    @GetMapping("/intenciones/{id}/historial")
    public List<EventoDto> historial(@PathVariable long id) {
        return conocimiento.historial(id).stream().map(e -> EventoDto.de(e, json)).toList();
    }

    @GetMapping("/dependencias")
    public List<Dependencia> dependencias() {
        return conocimiento.dependencias();
    }
}
