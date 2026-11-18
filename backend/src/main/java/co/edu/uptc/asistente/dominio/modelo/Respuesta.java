package co.edu.uptc.asistente.dominio.modelo;

import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;

import java.time.Instant;

/** Texto que ve el estudiante. Pertenece al agregado Intencion. */
public class Respuesta {
    public static final int MAX_CARACTERES = 1500;

    private Long id;
    private String texto;
    private boolean activa;
    private Instant fechaActualizacion;
    private Long actualizadoPor;

    public Respuesta(Long id, String texto, boolean activa, Instant fechaActualizacion, Long actualizadoPor) {
        this.id = id;
        this.texto = validarTexto(texto);
        this.activa = activa;
        this.fechaActualizacion = fechaActualizacion;
        this.actualizadoPor = actualizadoPor;
    }

    public static Respuesta nueva(String texto, Long usuarioId, Instant ahora) {
        return new Respuesta(null, texto, true, ahora, usuarioId);
    }

    public static String validarTexto(String texto) {
        String t = texto == null ? "" : texto.strip();
        if (t.isEmpty()) {
            throw new ValidacionException("texto", "El texto de la respuesta es obligatorio");
        }
        if (t.length() > MAX_CARACTERES) {
            throw new ValidacionException("texto", "La respuesta no puede superar " + MAX_CARACTERES + " caracteres");
        }
        return t;
    }

    public void editar(String nuevoTexto, Long usuarioId, Instant ahora) {
        this.texto = validarTexto(nuevoTexto);
        tocar(usuarioId, ahora);
    }

    public void cambiarEstado(boolean activa, Long usuarioId, Instant ahora) {
        this.activa = activa;
        tocar(usuarioId, ahora);
    }

    private void tocar(Long usuarioId, Instant ahora) {
        this.actualizadoPor = usuarioId;
        this.fechaActualizacion = ahora;
    }

    public Long getId() { return id; }
    public void asignarId(Long id) { this.id = id; }
    public String getTexto() { return texto; }
    public boolean isActiva() { return activa; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public Long getActualizadoPor() { return actualizadoPor; }
}
