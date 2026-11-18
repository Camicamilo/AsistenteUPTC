package co.edu.uptc.asistente.dominio.excepcion;

/** El recurso solicitado no existe (HTTP 404). */
public class NoEncontradoException extends DominioException {
    public NoEncontradoException(String mensaje) {
        super(mensaje, null);
    }
}
