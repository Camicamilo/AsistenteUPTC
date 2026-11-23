package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.evento.ConsultaEscalada;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;

/** Aviso al personal cuando una consulta no se resuelve (IF-06, UC-11 paso 7). */
public interface NotificadorEscalamiento {
    void notificar(ConsultaEscalada evento, Dependencia dependencia);

    /** Envía las credenciales temporales a un administrador nuevo (UC-14 paso 7). */
    void enviarCredenciales(String correo, String nombre, String contrasenaTemporal);
}
