package co.edu.uptc.asistente.infraestructura.entrada.rest.dto;

import co.edu.uptc.asistente.dominio.modelo.EventoAlmacenado;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.Respuesta;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

/** Cuerpos de las respuestas REST. Los nombres coinciden con lo que consume el frontend. */
public final class Respuestas {
    private Respuestas() {
    }

    public record UsuarioDto(Long id, String nombre, String correo, String rol, boolean activo,
                             boolean debeCambiarPassword, Instant fechaRegistro, Instant ultimoAcceso) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().valor(), u.isActivo(),
                    u.isDebeCambiarPassword(), u.getFechaRegistro(), u.getUltimoAcceso());
        }
    }

    public record LoginDto(String token, UsuarioDto usuario) {
    }

    public record UsuarioCreadoDto(UsuarioDto usuario, String contrasenaTemporal) {
    }

    public record ContrasenaTemporalDto(String contrasenaTemporal) {
    }

    public record RespuestaDto(Long id, String texto, boolean activa, Instant fechaActualizacion, Long actualizadoPor) {
        static RespuestaDto de(Respuesta r) {
            return new RespuestaDto(r.getId(), r.getTexto(), r.isActiva(), r.getFechaActualizacion(),
                    r.getActualizadoPor());
        }
    }

    public record IntencionDto(Long id, String nombre, String categoria, String descripcion, String dependencia,
                               boolean social, List<String> ejemplos, List<RespuestaDto> respuestas) {
        public static IntencionDto de(Intencion it) {
            return new IntencionDto(it.getId(), it.getNombre(), it.getCategoria(), it.getDescripcion(),
                    it.getDependencia(), it.isSocial(), it.getEjemplos(),
                    it.getRespuestas().stream().map(RespuestaDto::de).toList());
        }
    }

    public record EventoDto(Long id, int version, String tipoEvento, JsonNode datos, Instant fechaHora,
                            Long usuarioId) {
        public static EventoDto de(EventoAlmacenado e, ObjectMapper json) {
            JsonNode datos;
            try {
                datos = json.readTree(e.datos());
            } catch (Exception ex) {
                datos = json.createObjectNode();
            }
            return new EventoDto(e.id(), e.version(), e.tipoEvento(), datos, e.fechaHora(), e.usuarioId());
        }
    }

    public record ErrorDto(String mensaje, String campo, String codigo, Object dependencia) {
    }

    public record OkDto(boolean ok) {
    }
}
