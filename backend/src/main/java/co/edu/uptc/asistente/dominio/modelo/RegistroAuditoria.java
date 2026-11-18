package co.edu.uptc.asistente.dominio.modelo;

import java.time.Instant;

/** Traza de responsabilidad de las acciones del panel (no repudio). */
public record RegistroAuditoria(Long id, Instant fecha, String usuario, String accion, String detalle) {
}
