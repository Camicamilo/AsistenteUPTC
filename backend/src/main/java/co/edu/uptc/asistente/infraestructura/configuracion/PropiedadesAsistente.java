package co.edu.uptc.asistente.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Propiedades "asistente.*" de application.yml. */
@ConfigurationProperties(prefix = "asistente")
public record PropiedadesAsistente(String zonaHoraria, Pln pln, Jwt jwt, Cors cors, Correo correo) {

    public record Pln(String url, String token, int timeoutMs) {
    }

    public record Jwt(String secreto, int horas) {
    }

    public record Cors(List<String> origenes) {
    }

    public record Correo(boolean habilitado, String remitente, String destinoEscalamiento,
                         boolean usarCorreoDeDependencia) {
    }
}
