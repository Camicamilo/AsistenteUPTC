package co.edu.uptc.asistente.infraestructura.seguridad;

import co.edu.uptc.asistente.aplicacion.puerto.entrada.Autenticar;
import co.edu.uptc.asistente.infraestructura.configuracion.PropiedadesAsistente;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguridad (RNF-03): API sin estado, el chat es público y anónimo, todo /api/admin/** exige JWT.
 * No se usan cookies, por eso CSRF no aplica.
 */
@Configuration
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtServicio jwt, Autenticar autenticar)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/chat/**", "/api/admin/auth/login").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/usuarios/**", "/api/admin/auditoria/**")
                        .hasRole("SUPERADMINISTRADOR")
                        .requestMatchers("/api/admin/**", "/actuator/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                escribir(res, HttpServletResponse.SC_UNAUTHORIZED, "Sesión expirada o no válida"))
                        .accessDeniedHandler((req, res, ex) ->
                                escribir(res, HttpServletResponse.SC_FORBIDDEN, "No tienes permiso para esta acción")))
                .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                                + "script-src 'self' 'unsafe-inline'")))
                .addFilterBefore(new FiltroJwt(jwt, autenticar), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void escribir(HttpServletResponse res, int estado, String mensaje) throws IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }

    /** Orígenes permitidos (el frontend en otro dominio, por ejemplo Vercel). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(PropiedadesAsistente propiedades) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(propiedades.cors().origenes());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        c.setExposedHeaders(List.of("Content-Disposition"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", c);
        return fuente;
    }
}
