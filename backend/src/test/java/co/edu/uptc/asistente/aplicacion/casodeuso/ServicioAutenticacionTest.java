package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.AuditoriaEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.CodificadorFalso;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.NotificadorFalso;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.RelojFijo;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.TokensFalsos;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.UsuariosEnMemoria;
import co.edu.uptc.asistente.aplicacion.dto.UsuarioCreado;
import co.edu.uptc.asistente.dominio.excepcion.ConflictoException;
import co.edu.uptc.asistente.dominio.excepcion.NoAutorizadoException;
import co.edu.uptc.asistente.dominio.excepcion.ProhibidoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Rol;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Autenticación (IF-05) y gestión de usuarios (UC-14). */
class ServicioAutenticacionTest {
    private UsuariosEnMemoria usuarios;
    private RelojFijo reloj;
    private ServicioAutenticacion auth;
    private ServicioUsuarios gestion;
    private Usuario superadmin;
    private Usuario gestor;

    @BeforeEach
    void preparar() {
        usuarios = new UsuariosEnMemoria();
        reloj = new RelojFijo();
        CodificadorFalso cod = new CodificadorFalso();
        AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
        auth = new ServicioAutenticacion(usuarios, cod, new TokensFalsos(), auditoria, reloj);
        gestion = new ServicioUsuarios(usuarios, cod, new NotificadorFalso(), auditoria, reloj);
        superadmin = usuarios.guardar(new Usuario(null, "Admin General", "admin@uptc.edu.co", "hash:Admin2026*",
                Rol.SUPERADMINISTRADOR, true, false, Fakes.AHORA, null));
        gestor = usuarios.guardar(new Usuario(null, "Gestor", "gestor@uptc.edu.co", "hash:Gestor2026*",
                Rol.ADMINISTRADOR, true, false, Fakes.AHORA, null));
    }

    @Test
    void loginCorrectoDevuelveTokenYRegistraAcceso() {
        var r = auth.login(" ADMIN@uptc.edu.co ", "Admin2026*");
        assertEquals("token-" + superadmin.getId(), r.token());
        assertNotNull(usuarios.porId(superadmin.getId()).orElseThrow().getUltimoAcceso());
    }

    @Test
    void elMensajeDeErrorNoRevelaQueCampoFallo() {
        NoAutorizadoException a = assertThrows(NoAutorizadoException.class,
                () -> auth.login("admin@uptc.edu.co", "mala"));
        NoAutorizadoException b = assertThrows(NoAutorizadoException.class,
                () -> auth.login("noexiste@uptc.edu.co", "Admin2026*"));
        assertEquals(a.getMessage(), b.getMessage());
    }

    @Test
    void bloqueaDespuesDeCincoIntentosYDesbloqueaConElTiempo() {
        for (int i = 0; i < ServicioAutenticacion.MAX_INTENTOS; i++) {
            assertThrows(NoAutorizadoException.class, () -> auth.login("admin@uptc.edu.co", "mala"));
        }
        NoAutorizadoException bloqueado = assertThrows(NoAutorizadoException.class,
                () -> auth.login("admin@uptc.edu.co", "Admin2026*"));
        assertTrue(bloqueado.getMessage().contains("Demasiados intentos"));
        reloj.avanzarMinutos(6);
        assertNotNull(auth.login("admin@uptc.edu.co", "Admin2026*").token());
    }

    @Test
    void unUsuarioDesactivadoNoEntra() {
        gestion.actualizar(gestor.getId(), null, null, false, superadmin);
        assertThrows(NoAutorizadoException.class, () -> auth.login("gestor@uptc.edu.co", "Gestor2026*"));
        assertThrows(NoAutorizadoException.class, () -> auth.usuarioActivo(gestor.getId()));
    }

    @Test
    void soloElSuperadministradorGestionaUsuarios() {
        assertThrows(ProhibidoException.class, () -> gestion.listar(gestor));
        assertThrows(ProhibidoException.class, () -> gestion.crear("Nuevo", "nuevo@uptc.edu.co", "administrador", gestor));
    }

    @Test
    void creaUsuarioConContrasenaTemporalYSoloCorreoInstitucional() {
        UsuarioCreado c = gestion.crear("Nueva Persona", "nueva@uptc.edu.co", "administrador", superadmin);
        assertTrue(c.usuario().isDebeCambiarPassword());
        assertNotNull(auth.login("nueva@uptc.edu.co", c.contrasenaTemporal()).token());
        assertThrows(ValidacionException.class,
                () -> gestion.crear("Externo", "alguien@gmail.com", "administrador", superadmin));
        assertThrows(ConflictoException.class,
                () -> gestion.crear("Otra", "nueva@uptc.edu.co", "administrador", superadmin));
    }

    @Test
    void nadieSeDesactivaASiMismo() {
        assertThrows(ValidacionException.class, () -> gestion.actualizar(superadmin.getId(), null, null, false, superadmin));
        assertThrows(ValidacionException.class,
                () -> gestion.actualizar(superadmin.getId(), null, "administrador", null, superadmin));
    }

    @Test
    void cambioDeContrasenaExigeLaActualYUnaPoliticaMinima() {
        assertThrows(ValidacionException.class, () -> auth.cambiarContrasena(gestor, "mala", "NuevaClave123"));
        assertThrows(ValidacionException.class, () -> auth.cambiarContrasena(gestor, "Gestor2026*", "corta"));
        auth.cambiarContrasena(gestor, "Gestor2026*", "NuevaClave123");
        assertFalse(usuarios.porId(gestor.getId()).orElseThrow().isDebeCambiarPassword());
        assertNotNull(auth.login("gestor@uptc.edu.co", "NuevaClave123").token());
    }
}
