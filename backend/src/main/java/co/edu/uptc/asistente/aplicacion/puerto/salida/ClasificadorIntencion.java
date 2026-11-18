package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.modelo.ResultadoClasificacion;

/**
 * Puerto de salida hacia el motor PLN (IF-02). El núcleo no sabe si detrás hay Rasa,
 * Dialogflow o el servicio liviano: eso lo decide el adaptador.
 *
 * @throws co.edu.uptc.asistente.dominio.excepcion.PlnNoDisponibleException si el motor no responde
 */
public interface ClasificadorIntencion {
    ResultadoClasificacion clasificar(String texto);
}
