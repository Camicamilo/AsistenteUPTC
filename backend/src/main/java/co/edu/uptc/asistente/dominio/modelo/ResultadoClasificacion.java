package co.edu.uptc.asistente.dominio.modelo;

import java.util.List;

/** Lo que devuelve el motor PLN: intención, confianza y entidades (RF-02). */
public record ResultadoClasificacion(String intencion, NivelConfianza confianza, List<Entidad> entidades) {
    /** Nombre que usa Rasa (y nuestro servicio PLN) para las frases fuera del dominio. */
    public static final String FALLBACK = "nlu_fallback";

    public ResultadoClasificacion {
        entidades = entidades == null ? List.of() : List.copyOf(entidades);
    }

    public boolean esFueraDeDominio() {
        return intencion == null || intencion.isBlank() || FALLBACK.equals(intencion);
    }
}
