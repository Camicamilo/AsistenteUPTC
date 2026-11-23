package co.edu.uptc.asistente.dominio.modelo;

/** Oficina de la UPTC a la que se remite al estudiante cuando la consulta se escala (RD-04). */
public record Dependencia(String clave, String nombre, String correo, String telefono, String enlace, String horario) {
}
