package co.edu.uptc.asistente.dominio.modelo;

import java.util.List;

/** Lo que recibe el chat del estudiante: una respuesta o un escalamiento con la dependencia. */
public record RespuestaChat(
        String tipo,
        String texto,
        String intent,
        String categoria,
        Double confianza,
        List<Entidad> entidades,
        Dependencia dependencia,
        long tiempoMs) {

    public static final String RESPUESTA = "respuesta";
    public static final String ESCALAMIENTO = "escalamiento";

    public boolean esEscalamiento() {
        return ESCALAMIENTO.equals(tipo);
    }
}
