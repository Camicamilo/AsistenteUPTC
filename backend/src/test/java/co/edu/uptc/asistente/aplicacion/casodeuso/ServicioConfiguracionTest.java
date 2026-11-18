package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.AuditoriaEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.ConfiguracionEnMemoria;
import co.edu.uptc.asistente.dominio.excepcion.ProhibidoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Configuracion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.Rol;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** UC-16: el umbral solo se puede configurar entre 50 % y 80 %. */
class ServicioConfiguracionTest {
    private final ConfiguracionEnMemoria repo = new ConfiguracionEnMemoria();
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
    private final ServicioConfiguracion servicio = new ServicioConfiguracion(repo, auditoria);
    private final Usuario superadmin = new Usuario(1L, "Admin", "admin@uptc.edu.co", "x", Rol.SUPERADMINISTRADOR,
            true, false, Fakes.AHORA, null);
    private final Usuario gestor = new Usuario(2L, "Gestor", "gestor@uptc.edu.co", "x", Rol.ADMINISTRADOR,
            true, false, Fakes.AHORA, null);

    @Test
    void actualizaYAudita() {
        servicio.actualizar(new Configuracion(0.70, 20, "Escríbele a {dependencia}.", false), superadmin);
        assertEquals(0.70, servicio.obtener().umbralConfianza(), 1e-9);
        assertEquals("Configurar sistema", auditoria.datos.get(0).accion());
    }

    @Test
    void rangosValidos() {
        assertThrows(ValidacionException.class, () -> new Configuracion(0.49, 30, "{dependencia}", true));
        assertThrows(ValidacionException.class, () -> new Configuracion(0.81, 30, "{dependencia}", true));
        assertThrows(ValidacionException.class, () -> new Configuracion(0.60, 30, "sin marcador", true));
        new Configuracion(0.50, 5, "{dependencia}", true);
        new Configuracion(0.80, 240, "{dependencia}", true);
    }

    @Test
    void soloSuperadministrador() {
        assertThrows(ProhibidoException.class, () -> servicio.restaurar(gestor));
    }

    @Test
    void restauraValoresPorDefecto() {
        servicio.actualizar(new Configuracion(0.75, 10, "{dependencia}", false), superadmin);
        assertEquals(Configuracion.POR_DEFECTO, servicio.restaurar(superadmin));
    }

    @Test
    void validaDatosDeDependencia() {
        assertThrows(ValidacionException.class, () -> servicio.actualizarDependencia("registro",
                new Dependencia("registro", "Registro", "no-es-correo", "", "", ""), superadmin));
        assertThrows(ValidacionException.class, () -> servicio.actualizarDependencia("registro",
                new Dependencia("registro", "Registro", "", "", "javascript:alert(1)", ""), superadmin));
        Dependencia d = servicio.actualizarDependencia("registro",
                new Dependencia("registro", " Registro y Control Académico ", "r@uptc.edu.co", "", "https://x.co", ""),
                superadmin);
        assertEquals("Registro y Control Académico", d.nombre());
    }
}
