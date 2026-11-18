package co.edu.uptc.asistente.infraestructura;

import co.edu.uptc.asistente.dominio.modelo.Rol;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import co.edu.uptc.asistente.infraestructura.configuracion.PropiedadesAsistente;
import co.edu.uptc.asistente.infraestructura.seguridad.JwtServicio;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServicioTest {
    private static final Instant T = Instant.parse("2026-11-18T15:00:00Z");
    private final Usuario u = new Usuario(7L, "Admin", "admin@uptc.edu.co", "x", Rol.SUPERADMINISTRADOR, true, false,
            T, null);

    private JwtServicio servicio(String secreto, Instant ahora) {
        PropiedadesAsistente p = new PropiedadesAsistente("America/Bogota",
                new PropiedadesAsistente.Pln("http://x", "", 100), new PropiedadesAsistente.Jwt(secreto, 8),
                new PropiedadesAsistente.Cors(List.of()), new PropiedadesAsistente.Correo(false, "", "", false));
        return new JwtServicio(p, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    @Test
    void emiteYValida() {
        String token = servicio("secreto-de-prueba", T).emitir(u);
        assertEquals(Optional.of(7L), servicio("secreto-de-prueba", T.plus(Duration.ofHours(7))).validar(token));
    }

    @Test
    void expiraALasOchoHoras() {
        String token = servicio("secreto-de-prueba", T).emitir(u);
        assertTrue(servicio("secreto-de-prueba", T.plus(Duration.ofHours(8)).plusSeconds(1)).validar(token).isEmpty());
    }

    @Test
    void rechazaFirmaDeOtroSecretoOTokenAlterado() {
        String token = servicio("secreto-de-prueba", T).emitir(u);
        assertTrue(servicio("otro-secreto", T).validar(token).isEmpty());
        assertTrue(servicio("secreto-de-prueba", T).validar(token + "x").isEmpty());
        assertTrue(servicio("secreto-de-prueba", T).validar("basura").isEmpty());
    }
}
