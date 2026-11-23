package co.edu.uptc.asistente.dominio;

import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.MotivoEscalamiento;
import co.edu.uptc.asistente.dominio.modelo.NivelConfianza;
import co.edu.uptc.asistente.dominio.modelo.Respuesta;
import co.edu.uptc.asistente.dominio.modelo.ResultadoClasificacion;
import co.edu.uptc.asistente.dominio.modelo.SesionChat;
import co.edu.uptc.asistente.dominio.modelo.TextoConsulta;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import co.edu.uptc.asistente.dominio.servicio.PoliticaEscalamiento;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas del dominio (8.8.4). Valores límite y particiones de equivalencia. */
class DominioTest {
    private static final Instant T = Instant.parse("2026-11-18T15:00:00Z");

    private static Intencion intencion(boolean conRespuestaActiva) {
        Respuesta r = Respuesta.nueva("Texto", null, T);
        r.cambiarEstado(conRespuestaActiva, null, T);
        return new Intencion(1L, "consulta_matricula", "académico", "", "registro", false, List.of(), List.of(r));
    }

    private static ResultadoClasificacion resultado(String intent, double confianza) {
        return new ResultadoClasificacion(intent, NivelConfianza.de(confianza), List.of());
    }

    @Test
    void textoConsultaValoresLimite() {
        assertThrows(ValidacionException.class, () -> new TextoConsulta(""));
        assertThrows(ValidacionException.class, () -> new TextoConsulta(null));
        assertEquals(1, new TextoConsulta("a").valor().length());
        assertEquals(500, new TextoConsulta("a".repeat(500)).valor().length());
        assertThrows(ValidacionException.class, () -> new TextoConsulta("a".repeat(501)));
        assertEquals("hola", new TextoConsulta("  hola  ").valor());
    }

    @Test
    void politicaEscalamiento() {
        Intencion it = intencion(true);
        assertEquals(Optional.empty(), PoliticaEscalamiento.evaluar(resultado("consulta_matricula", 0.60), it, 0.60));
        assertEquals(Optional.of(MotivoEscalamiento.CONFIANZA_BAJA),
                PoliticaEscalamiento.evaluar(resultado("consulta_matricula", 0.59), it, 0.60));
        assertEquals(Optional.of(MotivoEscalamiento.FUERA_DEL_DOMINIO),
                PoliticaEscalamiento.evaluar(resultado("nlu_fallback", 0.99), it, 0.60));
        assertEquals(Optional.of(MotivoEscalamiento.FUERA_DEL_DOMINIO),
                PoliticaEscalamiento.evaluar(resultado("consulta_matricula", 0.99), null, 0.60));
        assertEquals(Optional.of(MotivoEscalamiento.SIN_RESPUESTA_ACTIVA),
                PoliticaEscalamiento.evaluar(resultado("consulta_matricula", 0.99), intencion(false), 0.60));
    }

    @Test
    void nivelConfianzaFueraDeRango() {
        assertThrows(ValidacionException.class, () -> new NivelConfianza(1.01));
        assertThrows(ValidacionException.class, () -> new NivelConfianza(-0.01));
        assertEquals(1.0, NivelConfianza.de(7).valor());
        assertEquals(0.92, NivelConfianza.de(0.9151).redondeado());
    }

    @Test
    void respuestaMaximo1500Caracteres() {
        Respuesta.validarTexto("a".repeat(1500));
        assertThrows(ValidacionException.class, () -> Respuesta.validarTexto("a".repeat(1501)));
    }

    @Test
    void nombreDeIntencion() {
        Intencion.validarNombre("info_admisiones2");
        assertThrows(ValidacionException.class, () -> Intencion.validarNombre("ab"));
        assertThrows(ValidacionException.class, () -> Intencion.validarNombre("Info Admisiones"));
        assertThrows(ValidacionException.class, () -> Intencion.validarNombre("1nombre"));
    }

    @Test
    void sesionCanalYCierre() {
        SesionChat s = SesionChat.iniciar("SES-1", "Móvil", T);
        assertEquals("móvil", s.getCanal());
        assertEquals("web", SesionChat.normalizarCanal("cualquier cosa"));
        assertFalse(s.inactivaDesde(T.plusSeconds(29 * 60), 30));
        assertTrue(s.inactivaDesde(T.plusSeconds(30 * 60), 30));
        assertTrue(s.cerrar(T, SesionChat.CIERRE_USUARIO));
        assertFalse(s.cerrar(T, SesionChat.CIERRE_USUARIO));
    }

    @Test
    void correoInstitucionalYContrasena() {
        assertEquals("ana.perez@uptc.edu.co", Usuario.normalizarCorreo(" Ana.Perez@UPTC.edu.co "));
        assertThrows(ValidacionException.class, () -> Usuario.normalizarCorreo("ana@gmail.com"));
        assertThrows(ValidacionException.class, () -> Usuario.normalizarCorreo("ana@uptc.edu.co.evil.com"));
        Usuario.validarContrasena("Segura123");
        assertThrows(ValidacionException.class, () -> Usuario.validarContrasena("sololetras"));
        assertThrows(ValidacionException.class, () -> Usuario.validarContrasena("12345678"));
    }
}
