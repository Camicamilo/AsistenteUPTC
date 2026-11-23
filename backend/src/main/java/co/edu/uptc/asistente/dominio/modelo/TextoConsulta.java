package co.edu.uptc.asistente.dominio.modelo;

import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;

/** Objeto de valor: texto de una consulta. No vacío y de máximo 500 caracteres (RF-01). */
public record TextoConsulta(String valor) {
    public static final int MAX_CARACTERES = 500;

    public TextoConsulta {
        valor = valor == null ? "" : valor.strip();
        if (valor.isEmpty()) {
            throw new ValidacionException("texto", "La consulta está vacía");
        }
        if (valor.length() > MAX_CARACTERES) {
            throw new ValidacionException("texto", "La consulta supera " + MAX_CARACTERES + " caracteres");
        }
    }
}
