package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;

import java.time.Instant;
import java.util.List;

public interface RepositorioConsultas {
    void registrar(RegistroConsulta registro);

    /** Consultas con fecha en [desde, hasta], ordenadas de la más reciente a la más antigua. */
    List<RegistroConsulta> entre(Instant desde, Instant hasta);
}
