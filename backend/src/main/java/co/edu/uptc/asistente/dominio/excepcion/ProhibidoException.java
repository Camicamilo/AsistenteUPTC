package co.edu.uptc.asistente.dominio.excepcion;

/** El usuario está autenticado pero su rol no le permite la operación (HTTP 403). */
public class ProhibidoException extends DominioException {
    public ProhibidoException(String mensaje) {
        super(mensaje, null);
    }
}
