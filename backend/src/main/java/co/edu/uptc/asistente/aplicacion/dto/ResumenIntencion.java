package co.edu.uptc.asistente.aplicacion.dto;

import co.edu.uptc.asistente.dominio.modelo.Intencion;

import java.time.Instant;

/** Fila de la tabla de la base de conocimiento (UC-12). */
public record ResumenIntencion(Long id, String nombre, String categoria, String descripcion, String dependencia,
                               long respuestasActivas, int totalRespuestas, Instant ultimaActualizacion) {
    public static ResumenIntencion de(Intencion it) {
        return new ResumenIntencion(it.getId(), it.getNombre(), it.getCategoria(), it.getDescripcion(),
                it.getDependencia(), it.respuestasActivas().size(), it.getRespuestas().size(),
                it.ultimaActualizacion());
    }
}
