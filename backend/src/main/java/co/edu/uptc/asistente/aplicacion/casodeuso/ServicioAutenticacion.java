package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.dto.ResultadoLogin;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.Autenticar;
import co.edu.uptc.asistente.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioAuditoria;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uptc.asistente.dominio.excepcion.NoAutorizadoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inicio de sesión del panel. El mensaje de error es genérico (no revela qué campo falló)
 * y después de 5 intentos fallidos el correo queda bloqueado 5 minutos (fuerza bruta).
 */
public class ServicioAutenticacion implements Autenticar {
    static final int MAX_INTENTOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(5);
    private static final String ERROR_GENERICO = "Usuario o contraseña incorrectos";

    private final RepositorioUsuarios usuarios;
    private final CodificadorContrasenas codificador;
    private final EmisorTokens tokens;
    private final RepositorioAuditoria auditoria;
    private final Clock reloj;
    private final Map<String, Intentos> intentos = new ConcurrentHashMap<>();

    private record Intentos(int fallidos, Instant bloqueadoHasta) {
    }

    public ServicioAutenticacion(RepositorioUsuarios usuarios, CodificadorContrasenas codificador, EmisorTokens tokens,
                                 RepositorioAuditoria auditoria, Clock reloj) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.tokens = tokens;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Override
    public ResultadoLogin login(String correo, String contrasena) {
        String clave = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        Instant ahora = reloj.instant();
        Intentos previos = intentos.get(clave);
        if (previos != null && previos.bloqueadoHasta() != null && ahora.isBefore(previos.bloqueadoHasta())) {
            throw new NoAutorizadoException("Demasiados intentos fallidos. Espera unos minutos e intenta de nuevo.");
        }
        Usuario u = usuarios.porCorreo(clave)
                .filter(Usuario::isActivo)
                .filter(x -> contrasena != null && codificador.coincide(contrasena, x.getPasswordHash()))
                .orElse(null);
        if (u == null) {
            registrarFallo(clave, ahora);
            throw new NoAutorizadoException(ERROR_GENERICO);
        }
        intentos.remove(clave);
        u.registrarAcceso(ahora);
        usuarios.guardar(u);
        auditoria.registrar(u.getCorreo(), "Inicio de sesión", "Acceso al panel administrativo");
        return new ResultadoLogin(tokens.emitir(u), u);
    }

    private void registrarFallo(String clave, Instant ahora) {
        intentos.compute(clave, (k, v) -> {
            int n = (v == null || (v.bloqueadoHasta() != null && !ahora.isBefore(v.bloqueadoHasta()))) ? 1
                    : v.fallidos() + 1;
            return new Intentos(n, n >= MAX_INTENTOS ? ahora.plus(BLOQUEO) : null);
        });
    }

    @Override
    public Usuario usuarioActivo(long usuarioId) {
        return usuarios.porId(usuarioId).filter(Usuario::isActivo)
                .orElseThrow(() -> new NoAutorizadoException("Sesión no válida"));
    }

    @Override
    public void cambiarContrasena(Usuario actor, String actual, String nueva) {
        Autorizacion.exigirSesion(actor);
        if (actual == null || !codificador.coincide(actual, actor.getPasswordHash())) {
            throw new ValidacionException("actual", "La contraseña actual no es correcta");
        }
        Usuario.validarContrasena(nueva);
        if (nueva.equals(actual)) {
            throw new ValidacionException("nueva", "La nueva contraseña debe ser diferente a la actual");
        }
        actor.cambiarContrasena(codificador.codificar(nueva), false);
        usuarios.guardar(actor);
        auditoria.registrar(actor.getCorreo(), "Cambiar contraseña", "Cambio de contraseña propia");
    }
}
