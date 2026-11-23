package co.edu.uptc.asistente.infraestructura.seguridad;

import co.edu.uptc.asistente.aplicacion.puerto.entrada.Autenticar;
import co.edu.uptc.asistente.dominio.excepcion.NoAutorizadoException;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Lee "Authorization: Bearer &lt;JWT&gt;", valida firma y vigencia, y verifica que el usuario siga
 * activo (si un superadministrador lo desactiva, su token deja de servir de inmediato).
 */
public class FiltroJwt extends OncePerRequestFilter {
    private final JwtServicio jwt;
    private final Autenticar autenticar;

    public FiltroJwt(JwtServicio jwt, Autenticar autenticar) {
        this.jwt = jwt;
        this.autenticar = autenticar;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            jwt.validar(cabecera.substring(7).strip()).ifPresent(id -> {
                try {
                    Usuario u = autenticar.usuarioActivo(id);
                    var rol = new SimpleGrantedAuthority("ROLE_" + u.getRol().name().toUpperCase(Locale.ROOT));
                    var auth = new UsernamePasswordAuthenticationToken(u, null, List.of(rol));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (NoAutorizadoException e) {
                    SecurityContextHolder.clearContext();
                }
            });
        }
        chain.doFilter(request, response);
    }
}
