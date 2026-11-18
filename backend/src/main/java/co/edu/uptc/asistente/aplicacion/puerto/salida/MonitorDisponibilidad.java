package co.edu.uptc.asistente.aplicacion.puerto.salida;

import java.time.Instant;
import java.util.OptionalDouble;

/** Fracción de chequeos de salud exitosos en un periodo (RNF-02). */
public interface MonitorDisponibilidad {
    OptionalDouble disponibilidad(Instant desde, Instant hasta);
}
