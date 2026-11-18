package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.AuditoriaEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.ConfiguracionEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.EntrenadorFalso;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.EventosEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.IntencionesEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.RelojFijo;
import co.edu.uptc.asistente.aplicacion.dto.NuevaIntencion;
import co.edu.uptc.asistente.dominio.evento.ConocimientoActualizado;
import co.edu.uptc.asistente.dominio.excepcion.ConflictoException;
import co.edu.uptc.asistente.dominio.excepcion.NoEncontradoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.Rol;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioConocimientoTest {
    private IntencionesEnMemoria intenciones;
    private EventosEnMemoria eventos;
    private EntrenadorFalso entrenador;
    private AuditoriaEnMemoria auditoria;
    private ServicioConocimiento servicio;
    private final Usuario gestor = new Usuario(2L, "Gestor", "gestor@uptc.edu.co", "x", Rol.ADMINISTRADOR, true,
            false, Fakes.AHORA, null);

    @BeforeEach
    void preparar() {
        intenciones = new IntencionesEnMemoria();
        eventos = new EventosEnMemoria();
        entrenador = new EntrenadorFalso();
        auditoria = new AuditoriaEnMemoria();
        servicio = new ServicioConocimiento(intenciones, new ConfiguracionEnMemoria(), eventos, eventos, entrenador,
                auditoria, new RelojFijo());
        intenciones.agregar("consulta_matricula", "académico", "registro", false, "Respuesta A", "Respuesta B");
        intenciones.agregar("saludo", "administrativo", "general", true, "Hola");
    }

    private NuevaIntencion nueva(String nombre, List<String> ejemplos) {
        return new NuevaIntencion(nombre, "académico", "Prueba", "registro", "Texto de la respuesta", ejemplos);
    }

    @Test
    void creaIntencionAuditaYPideReentrenar() {
        Intencion it = servicio.crear(nueva("fechas_grado", List.of("¿cuándo es el grado?", "fechas de grado",
                " ", "ceremonia de grado", "fechas de grado")), gestor);
        assertEquals(3, it.getEjemplos().size()); // sin vacíos ni repetidos
        assertEquals(1, it.respuestasActivas().size());
        assertEquals("Crear intención", auditoria.datos.get(0).accion());
        ConocimientoActualizado e = (ConocimientoActualizado) eventos.publicados.get(0);
        assertEquals("IntencionCreada", e.tipoEvento());
        assertTrue(e.requiereReentrenar());
    }

    @Test
    void validaNombreDuplicadoYEjemplos() {
        assertThrows(ConflictoException.class, () -> servicio.crear(nueva("consulta_matricula",
                List.of("a b", "c d", "e f")), gestor));
        ValidacionException sinEjemplos = assertThrows(ValidacionException.class,
                () -> servicio.crear(nueva("nueva_intencion", List.of("solo uno")), gestor));
        assertEquals("ejemplos", sinEjemplos.getCampo());
        ValidacionException nombre = assertThrows(ValidacionException.class,
                () -> servicio.crear(nueva("Con Espacios", List.of("a", "b", "c")), gestor));
        assertEquals("nombre", nombre.getCampo());
    }

    @Test
    void desactivarUnaRespuestaNoLaBorra() {
        Intencion it = intenciones.porNombre("consulta_matricula").orElseThrow();
        long rid = it.getRespuestas().get(0).getId();
        Intencion r = servicio.actualizarRespuesta(it.getId(), rid, null, false, gestor);
        assertEquals(2, r.getRespuestas().size());
        assertEquals(1, r.respuestasActivas().size());
        assertEquals("RespuestaDesactivada", eventos.publicados.get(0).tipoEvento());
        assertEquals("Desactivar respuesta", auditoria.datos.get(0).accion());
    }

    @Test
    void historialSaleDelAlmacenDeEventos() {
        Intencion it = intenciones.porNombre("consulta_matricula").orElseThrow();
        servicio.agregarRespuesta(it.getId(), "Respuesta C", gestor);
        servicio.actualizar(it.getId(), "administrativo", "nueva descripción", null, gestor);
        assertEquals(List.of("RespuestaCreada", "IntencionEditada"),
                servicio.historial(it.getId()).stream().map(e -> e.tipoEvento()).toList());
    }

    @Test
    void listarFiltraPorCategoriaYTexto() {
        assertEquals(1, servicio.listar(null, "académico", 1, 10).total());
        assertEquals(1, servicio.listar("respuesta b", null, 1, 10).total());
        assertEquals(2, servicio.listar("", "", 1, 10).total());
    }

    @Test
    void dependenciaInexistenteEsError() {
        long id = intenciones.porNombre("consulta_matricula").orElseThrow().getId();
        assertThrows(ValidacionException.class, () -> servicio.actualizar(id, null, null, "no_existe", gestor));
        assertThrows(NoEncontradoException.class, () -> servicio.obtener(999));
    }

    @Test
    void sincronizaElModeloConLosEjemplos() {
        servicio.sincronizarModelo();
        assertEquals(2, entrenador.ultimo.size());
        assertFalse(entrenador.ultimo.get("saludo").isEmpty());
    }
}
