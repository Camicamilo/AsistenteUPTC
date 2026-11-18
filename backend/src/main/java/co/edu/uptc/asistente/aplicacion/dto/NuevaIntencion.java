package co.edu.uptc.asistente.aplicacion.dto;

import java.util.List;

public record NuevaIntencion(String nombre, String categoria, String descripcion, String dependencia,
                             String respuesta, List<String> ejemplos) {
}
