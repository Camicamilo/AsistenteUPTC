package co.edu.uptc.asistente.aplicacion.puerto.entrada;

import co.edu.uptc.asistente.aplicacion.dto.UsuarioCreado;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.util.List;

/** Cuentas del panel; solo el superadministrador (UC-14). */
public interface GestionarUsuarios {
    List<Usuario> listar(Usuario actor);

    UsuarioCreado crear(String nombre, String correo, String rol, Usuario actor);

    Usuario actualizar(long id, String nombre, String rol, Boolean activo, Usuario actor);

    String restablecerContrasena(long id, Usuario actor);
}
