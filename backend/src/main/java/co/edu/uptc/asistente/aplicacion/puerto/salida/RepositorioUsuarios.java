package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.util.List;
import java.util.Optional;

public interface RepositorioUsuarios {
    Optional<Usuario> porId(long id);

    Optional<Usuario> porCorreo(String correo);

    List<Usuario> listar();

    Usuario guardar(Usuario usuario);
}
