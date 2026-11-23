package co.edu.uptc.asistente.dominio.excepcion;

/** Credenciales inválidas o sesión vencida (HTTP 401). */
public class NoAutorizadoException extends DominioException {
    public NoAutorizadoException(String mensaje) {
        super(mensaje, null);
    }
}
