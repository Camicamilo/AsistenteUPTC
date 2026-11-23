package co.edu.uptc.asistente.aplicacion.puerto.entrada;

import co.edu.uptc.asistente.aplicacion.dto.NuevaSesion;
import co.edu.uptc.asistente.dominio.modelo.TemaRapido;

import java.util.List;

/** Sesiones anónimas del chat (UC-04, UC-09). */
public interface GestionarSesiones {
    NuevaSesion iniciar(String canal);

    void cerrar(String sesionId);

    /** Cierra las sesiones sin actividad durante el tiempo configurado. Devuelve cuántas cerró. */
    int cerrarInactivas();

    List<TemaRapido> temas();
}
