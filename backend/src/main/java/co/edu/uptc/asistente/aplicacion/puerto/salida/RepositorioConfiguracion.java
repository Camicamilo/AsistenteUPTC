package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.modelo.Configuracion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.TemaRapido;

import java.util.List;
import java.util.Optional;

/** Configuración del sistema, dependencias y temas rápidos. */
public interface RepositorioConfiguracion {
    Configuracion obtener();

    void guardar(Configuracion configuracion);

    List<Dependencia> dependencias();

    Optional<Dependencia> dependencia(String clave);

    void guardarDependencia(Dependencia dependencia);

    List<TemaRapido> temasRapidos();
}
