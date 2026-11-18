package co.edu.uptc.asistente.dominio.modelo;

/** Por qué una consulta no se respondió automáticamente (UC-10 S1, UC-11). */
public enum MotivoEscalamiento {
    FUERA_DEL_DOMINIO("Fuera del dominio"),
    CONFIANZA_BAJA("Confianza por debajo del umbral"),
    SIN_RESPUESTA_ACTIVA("Sin respuesta activa"),
    PLN_NO_DISPONIBLE("Motor PLN no disponible");

    private final String etiqueta;

    MotivoEscalamiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
