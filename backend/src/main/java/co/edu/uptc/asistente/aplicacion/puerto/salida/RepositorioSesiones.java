package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.modelo.SesionChat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RepositorioSesiones {
    void guardar(SesionChat sesion);

    Optional<SesionChat> porId(String sesionId);

    /** Sesiones abiertas cuya última actividad es anterior a {@code limite}. */
    List<SesionChat> abiertasSinActividadDesde(Instant limite);
}
