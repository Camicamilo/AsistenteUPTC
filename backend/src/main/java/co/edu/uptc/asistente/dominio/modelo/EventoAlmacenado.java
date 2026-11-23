package co.edu.uptc.asistente.dominio.modelo;

import java.time.Instant;

/** Evento guardado en el almacén de eventos (solo inserción, EventSourcing 8.5). */
public record EventoAlmacenado(Long id, String agregadoId, String tipoAgregado, int version, String tipoEvento,
                               String datos, Instant fechaHora, Long usuarioId) {
}
