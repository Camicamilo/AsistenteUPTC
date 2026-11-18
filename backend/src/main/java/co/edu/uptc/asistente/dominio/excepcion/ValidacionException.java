package co.edu.uptc.asistente.dominio.excepcion;

/** Datos de entrada que no cumplen una regla de negocio (HTTP 400). */
public class ValidacionException extends DominioException {
    public ValidacionException(String campo, String mensaje) {
        super(mensaje, campo);
    }

    public ValidacionException(String mensaje) {
        super(mensaje, null);
    }
}
