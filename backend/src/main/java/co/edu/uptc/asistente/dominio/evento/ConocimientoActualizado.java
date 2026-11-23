package co.edu.uptc.asistente.dominio.evento;

import java.time.Instant;

/**
 * Cambio en la base de conocimiento. {@code accion} es el tipo de evento:
 * IntencionCreada, IntencionEditada, RespuestaCreada, RespuestaEditada, RespuestaActivada, RespuestaDesactivada.
 */
public record ConocimientoActualizado(Long intentId, String intent, String accion, Long respuestaId, String texto,
                                      Long usuarioId, Instant fechaHora, boolean requiereReentrenar)
        implements EventoDominio {
    public String agregadoId() { return "INT-" + intentId; }
    public String tipoAgregado() { return "Intencion"; }
    public String tipoEvento() { return accion; }
}
