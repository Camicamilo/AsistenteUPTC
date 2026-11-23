package co.edu.uptc.asistente.aplicacion.dto;

import co.edu.uptc.asistente.dominio.modelo.Usuario;

/** La contraseña temporal se muestra una sola vez al superadministrador (y se envía por correo si está activo). */
public record UsuarioCreado(Usuario usuario, String contrasenaTemporal) {
}
