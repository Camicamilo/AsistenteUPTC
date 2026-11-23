package co.edu.uptc.asistente.infraestructura.entrada.rest;

import co.edu.uptc.asistente.aplicacion.dto.ResultadoLogin;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.Autenticar;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.CambioContrasenaPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Peticiones.LoginPeticion;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.LoginDto;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.UsuarioDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Acceso al panel con JWT (IF-05)")
@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {
    private final Autenticar autenticar;

    public AuthController(Autenticar autenticar) {
        this.autenticar = autenticar;
    }

    @Operation(summary = "Inicia sesión y devuelve un JWT de 8 horas")
    @PostMapping("/login")
    public LoginDto login(@RequestBody LoginPeticion peticion) {
        ResultadoLogin r = autenticar.login(peticion.correo(), peticion.password());
        return new LoginDto(r.token(), UsuarioDto.de(r.usuario()));
    }

    @Operation(summary = "Datos del usuario de la sesión")
    @GetMapping("/yo")
    public UsuarioDto yo(@AuthenticationPrincipal Usuario actor) {
        return UsuarioDto.de(actor);
    }

    @Operation(summary = "Cambia la contraseña propia")
    @PutMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@AuthenticationPrincipal Usuario actor,
                                  @RequestBody CambioContrasenaPeticion peticion) {
        autenticar.cambiarContrasena(actor, peticion.actual(), peticion.nueva());
    }
}
