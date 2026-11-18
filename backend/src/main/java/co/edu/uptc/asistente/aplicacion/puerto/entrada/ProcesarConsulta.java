package co.edu.uptc.asistente.aplicacion.puerto.entrada;

import co.edu.uptc.asistente.dominio.modelo.RespuestaChat;

/** Puerto de entrada principal: recibir una consulta y devolver respuesta o escalamiento (UC-05). */
public interface ProcesarConsulta {
    RespuestaChat procesar(String sesionId, String texto);
}
