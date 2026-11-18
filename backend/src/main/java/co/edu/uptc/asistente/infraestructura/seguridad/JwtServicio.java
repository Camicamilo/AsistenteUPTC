package co.edu.uptc.asistente.infraestructura.seguridad;

import co.edu.uptc.asistente.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import co.edu.uptc.asistente.infraestructura.configuracion.PropiedadesAsistente;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Emite y valida el JWT del panel (HS256, expira a las 8 horas). */
@Component
public class JwtServicio implements EmisorTokens {
    private final SecretKey llave;
    private final Duration vigencia;
    private final Clock reloj;

    public JwtServicio(PropiedadesAsistente propiedades, Clock reloj) {
        this.llave = Keys.hmacShaKeyFor(sha256(propiedades.jwt().secreto()));
        this.vigencia = Duration.ofHours(propiedades.jwt().horas());
        this.reloj = reloj;
    }

    /** Deriva siempre una llave de 256 bits, sin importar el largo del secreto configurado. */
    private static byte[] sha256(String secreto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secreto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String emitir(Usuario u) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("nombre", u.getNombre())
                .claim("correo", u.getCorreo())
                .claim("rol", u.getRol().valor())
                .claim("debeCambiarPassword", u.isDebeCambiarPassword())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(llave)
                .compact();
    }

    /** Devuelve el id del usuario si el token es válido y está vigente. */
    public Optional<Long> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(llave)
                    .clock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
