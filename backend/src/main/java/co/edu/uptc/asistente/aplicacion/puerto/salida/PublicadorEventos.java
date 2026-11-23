package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.evento.EventoDominio;

/** Bus de eventos (patrón Observer / orientado a eventos, 8.4.4). */
public interface PublicadorEventos {
    void publicar(EventoDominio evento);
}
