package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.modelo.Intencion;

import java.util.List;
import java.util.Optional;

/** Persistencia del agregado Intencion (IF-03). */
public interface RepositorioIntenciones {
    List<Intencion> listar();

    Optional<Intencion> porId(long id);

    Optional<Intencion> porNombre(String nombre);

    boolean existeNombre(String nombre);

    /** Guarda el agregado completo (intención, ejemplos y respuestas) y lo devuelve con ids asignados. */
    Intencion guardar(Intencion intencion);
}
