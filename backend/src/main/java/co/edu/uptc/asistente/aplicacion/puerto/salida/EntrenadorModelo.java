package co.edu.uptc.asistente.aplicacion.puerto.salida;

import java.util.List;
import java.util.Map;

/** Envía los ejemplos de entrenamiento al motor PLN (intención -> ejemplos). */
public interface EntrenadorModelo {
    void entrenar(Map<String, List<String>> ejemplosPorIntencion);
}
