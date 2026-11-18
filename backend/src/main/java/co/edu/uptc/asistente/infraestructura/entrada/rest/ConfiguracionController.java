package co.edu.uptc.asistente.infraestructura.entrada.rest;

import co.edu.uptc.asistente.aplicacion.puerto.entrada.ConfigurarSistema;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarConocimiento;
import co.edu.uptc.asistente.dominio.modelo.Configuracion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.RegistroAuditoria;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.ConfiguracionPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.DependenciaPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.OkDto;
import co.edu.uptc.asistente.infraestructura.salida.pln.RasaAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Configuración del sistema (UC-16), dependencias, auditoría y estado del motor PLN. */
@Tag(name = "Configuración", description = "Parámetros del sistema (UC-16) y auditoría")
@RestController
@RequestMapping("/api/admin")
public class ConfiguracionController {
    private final ConfigurarSistema sistema;
    private final GestionarConocimiento conocimiento;
    private final RasaAdapter pln;

    public ConfiguracionController(ConfigurarSistema sistema, GestionarConocimiento conocimiento, RasaAdapter pln) {
        this.sistema = sistema;
        this.conocimiento = conocimiento;
        this.pln = pln;
    }

    @GetMapping("/configuracion")
    public Configuracion obtener() {
        return sistema.obtener();
    }

    @Operation(summary = "Cambia umbral (50–80 %), inactividad, plantilla y notificaciones sin reiniciar")
    @PutMapping("/configuracion")
    public Configuracion actualizar(@AuthenticationPrincipal Usuario actor, @RequestBody ConfiguracionPeticion p) {
        Configuracion actual = sistema.obtener();
        Configuracion nueva = new Configuracion(
                p.umbralConfianza() == null ? actual.umbralConfianza() : p.umbralConfianza(),
                p.inactividadSesionMin() == null ? actual.inactividadSesionMin() : p.inactividadSesionMin(),
                p.plantillaEscalamiento() == null ? actual.plantillaEscalamiento() : p.plantillaEscalamiento(),
                p.notificarPorCorreo() == null ? actual.notificarPorCorreo() : p.notificarPorCorreo());
        return sistema.actualizar(nueva, actor);
    }

    @PostMapping("/configuracion/restaurar")
    public Configuracion restaurar(@AuthenticationPrincipal Usuario actor) {
        return sistema.restaurar(actor);
    }

    @PutMapping("/dependencias/{clave}")
    public Dependencia actualizarDependencia(@AuthenticationPrincipal Usuario actor, @PathVariable String clave,
                                             @RequestBody DependenciaPeticion p) {
        return sistema.actualizarDependencia(clave,
                new Dependencia(clave, p.nombre(), p.correo(), p.telefono(), p.enlace(), p.horario()), actor);
    }

    @GetMapping("/auditoria")
    public List<RegistroAuditoria> auditoria(@AuthenticationPrincipal Usuario actor,
                                             @RequestParam(defaultValue = "100") int limite) {
        return sistema.auditoria(limite, actor);
    }

    @Operation(summary = "Estado del motor PLN y del circuit breaker")
    @GetMapping("/pln/estado")
    public Map<String, Object> estadoPln() {
        return Map.of("disponible", pln.saludable(), "circuito", pln.estadoCircuito());
    }

    @Operation(summary = "Reentrena el motor PLN con la base de conocimiento actual")
    @PostMapping("/pln/reentrenar")
    public OkDto reentrenar() {
        conocimiento.sincronizarModelo();
        return new OkDto(true);
    }
}
