package co.edu.uptc.asistente.dominio.excepcion;

/** El recurso ya existe o el estado no permite la operación (HTTP 409). */
public class ConflictoException extends DominioException {
    public ConflictoException(String campo, String mensaje) {
        super(mensaje, campo);
    }
}
