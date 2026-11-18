package co.edu.uptc.asistente.dominio.excepcion;

/** El motor PLN no respondió a tiempo o está caído (HTTP 503, RNF-08). */
public class PlnNoDisponibleException extends DominioException {
    public PlnNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, null);
        if (causa != null) {
            initCause(causa);
        }
    }
}
