package co.edu.uptc.asistente.infraestructura.entrada.rest.dto;

import java.util.List;

/** Cuerpos de las peticiones REST (la validación de negocio la hace el dominio). */
public final class Peticiones {
    private Peticiones() {
    }

    public record NuevaSesionPeticion(String canal) {
    }

    public record ConsultaPeticion(String sesionId, String texto) {
    }

    public record LoginPeticion(String correo, String password) {
    }

    public record CambioContrasenaPeticion(String actual, String nueva) {
    }

    public record NuevaIntencionPeticion(String nombre, String categoria, String descripcion, String dependencia,
                                         String respuesta, List<String> ejemplos) {
    }

    public record EditarIntencionPeticion(String categoria, String descripcion, String dependencia) {
    }

    public record TextoPeticion(String texto) {
    }

    public record EditarRespuestaPeticion(String texto, Boolean activa) {
    }

    public record NuevoUsuarioPeticion(String nombre, String correo, String rol) {
    }

    public record EditarUsuarioPeticion(String nombre, String rol, Boolean activo) {
    }

    public record ConfiguracionPeticion(Double umbralConfianza, Integer inactividadSesionMin,
                                        String plantillaEscalamiento, Boolean notificarPorCorreo) {
    }

    public record DependenciaPeticion(String nombre, String correo, String telefono, String enlace, String horario) {
    }
}
