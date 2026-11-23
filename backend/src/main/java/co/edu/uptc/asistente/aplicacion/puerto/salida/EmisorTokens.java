package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.modelo.Usuario;

/** Emite el JWT del panel (8 horas). */
public interface EmisorTokens {
    String emitir(Usuario usuario);
}
