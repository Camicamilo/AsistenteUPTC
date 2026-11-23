package co.edu.uptc.asistente.aplicacion.dto;

import co.edu.uptc.asistente.dominio.modelo.Usuario;

public record ResultadoLogin(String token, Usuario usuario) {
}
