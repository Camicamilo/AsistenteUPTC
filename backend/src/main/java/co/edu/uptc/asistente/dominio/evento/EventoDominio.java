package co.edu.uptc.asistente.dominio.evento;

import java.time.Instant;

/** Algo importante que ya pasó en el dominio. Se publica y se guarda en el almacén de eventos. */
public interface EventoDominio {
    String agregadoId();

    String tipoAgregado();

    Instant fechaHora();

    default String tipoEvento() {
        return getClass().getSimpleName();
    }

    /** Administrador responsable (solo en eventos de la base de conocimiento). */
    default Long usuarioId() {
        return null;
    }
}
