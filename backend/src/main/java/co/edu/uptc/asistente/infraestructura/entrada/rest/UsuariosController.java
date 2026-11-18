package co.edu.uptc.asistente.infraestructura.entrada.rest;

import co.edu.uptc.asistente.aplicacion.dto.UsuarioCreado;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarUsuarios;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.EditarUsuarioPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.NuevoUsuarioPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.ContrasenaTemporalDto;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.UsuarioCreadoDto;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.UsuarioDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Cuentas del panel (UC-14) — UserManager. Solo superadministrador. */
@Tag(name = "Usuarios", description = "Cuentas del panel (UC-14), solo superadministrador")
@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuariosController {
    private final GestionarUsuarios usuarios;

    public UsuariosController(GestionarUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<UsuarioDto> listar(@AuthenticationPrincipal Usuario actor) {
        return usuarios.listar(actor).stream().map(UsuarioDto::de).toList();
    }

    @Operation(summary = "Crea un administrador con contraseña temporal (se muestra una sola vez)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCreadoDto crear(@AuthenticationPrincipal Usuario actor, @RequestBody NuevoUsuarioPeticion p) {
        UsuarioCreado c = usuarios.crear(p.nombre(), p.correo(), p.rol(), actor);
        return new UsuarioCreadoDto(UsuarioDto.de(c.usuario()), c.contrasenaTemporal());
    }

    @Operation(summary = "Edita nombre, rol o estado (desactivar en lugar de borrar)")
    @PatchMapping("/{id}")
    public UsuarioDto actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable long id,
                                 @RequestBody EditarUsuarioPeticion p) {
        return UsuarioDto.de(usuarios.actualizar(id, p.nombre(), p.rol(), p.activo(), actor));
    }

    @PostMapping("/{id}/restablecer")
    public ContrasenaTemporalDto restablecer(@AuthenticationPrincipal Usuario actor, @PathVariable long id) {
        return new ContrasenaTemporalDto(usuarios.restablecerContrasena(id, actor));
    }
}
