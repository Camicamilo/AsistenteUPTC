package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.evento.EventoDominio;
import co.edu.uptc.asistente.dominio.modelo.EventoAlmacenado;

import java.util.List;

/** Almacén de eventos de solo inserción (EventSourcing 8.5). */
public interface AlmacenEventos {
    void agregar(EventoDominio evento);

    List<EventoAlmacenado> delAgregado(String agregadoId);
}
