package co.edu.uptc.asistente.dominio.excepcion;

/** Excepción base del dominio. Cada subclase se traduce a un código HTTP en el adaptador REST. */
public abstract class DominioException extends RuntimeException {
    private final String campo;

    protected DominioException(String mensaje, String campo) {
        super(mensaje);
        this.campo = campo;
    }

    /** Campo del formulario que causó el error (puede ser null). */
    public String getCampo() {
        return campo;
    }
}
