package co.edu.uptc.asistente.dominio.modelo;

import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;

/** Parámetros operativos que el superadministrador ajusta sin reiniciar (UC-16). */
public record Configuracion(double umbralConfianza, int inactividadSesionMin, String plantillaEscalamiento,
                            boolean notificarPorCorreo) {
    public static final double UMBRAL_MIN = 0.50;
    public static final double UMBRAL_MAX = 0.80;
    public static final Configuracion POR_DEFECTO = new Configuracion(0.60, 30,
            "No tengo información suficiente para responder esa pregunta con seguridad. "
                    + "Te recomiendo comunicarte con {dependencia}.", true);

    public Configuracion {
        if (Double.isNaN(umbralConfianza) || umbralConfianza < UMBRAL_MIN || umbralConfianza > UMBRAL_MAX) {
            throw new ValidacionException("umbralConfianza", "El umbral de confianza debe estar entre 50 % y 80 %");
        }
        if (inactividadSesionMin < 5 || inactividadSesionMin > 240) {
            throw new ValidacionException("inactividadSesionMin", "La inactividad debe estar entre 5 y 240 minutos");
        }
        plantillaEscalamiento = plantillaEscalamiento == null ? "" : plantillaEscalamiento.strip();
        if (!plantillaEscalamiento.contains("{dependencia}") || plantillaEscalamiento.length() > 500) {
            throw new ValidacionException("plantillaEscalamiento",
                    "La plantilla debe incluir {dependencia} y tener máximo 500 caracteres");
        }
    }

    public String mensajeEscalamiento(Dependencia dependencia) {
        return plantillaEscalamiento.replace("{dependencia}", dependencia == null ? "la universidad" : dependencia.nombre());
    }
}
