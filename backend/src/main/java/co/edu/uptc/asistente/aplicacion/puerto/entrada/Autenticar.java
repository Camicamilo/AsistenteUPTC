package co.edu.uptc.asistente.aplicacion.puerto.entrada;

import co.edu.uptc.asistente.aplicacion.dto.ResultadoLogin;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

/** Acceso al panel con JWT (IF-05). */
public interface Autenticar {
    ResultadoLogin login(String correo, String contrasena);

    /** Usuario activo dueño de un token válido; lanza NoAutorizadoException si no lo es. */
    Usuario usuarioActivo(long usuarioId);

    void cambiarContrasena(Usuario actor, String actual, String nueva);
}
