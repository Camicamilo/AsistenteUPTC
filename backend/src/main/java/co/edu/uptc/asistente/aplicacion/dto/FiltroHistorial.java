package co.edu.uptc.asistente.aplicacion.dto;

/** Filtros del historial de consultas. estado: "resuelta", "escalada" o vacío. */
public record FiltroHistorial(String rango, String estado, String q, int page, int pageSize) {
}
