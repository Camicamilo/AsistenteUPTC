package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.ClasificadorFalso;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.ConfiguracionEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.ConsultasEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.EventosEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.IntencionesEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.RelojFijo;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.SesionesEnMemoria;
import co.edu.uptc.asistente.dominio.evento.ConsultaEscalada;
import co.edu.uptc.asistente.dominio.evento.ConsultaResuelta;
import co.edu.uptc.asistente.dominio.evento.SesionCerrada;
import co.edu.uptc.asistente.dominio.excepcion.ConflictoException;
import co.edu.uptc.asistente.dominio.excepcion.PlnNoDisponibleException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;
import co.edu.uptc.asistente.dominio.modelo.RespuestaChat;
import co.edu.uptc.asistente.dominio.servicio.SelectorRespuesta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de caja blanca del caso de uso Procesar Consulta (UC-05 a UC-11). */
class ServicioConversacionTest {
    private ClasificadorFalso pln;
    private IntencionesEnMemoria intenciones;
    private SesionesEnMemoria sesiones;
    private ConsultasEnMemoria consultas;
    private ConfiguracionEnMemoria configuracion;
    private EventosEnMemoria eventos;
    private RelojFijo reloj;
    private ServicioConversacion servicio;
    private String sesion;

    @BeforeEach
    void preparar() {
        pln = new ClasificadorFalso();
        intenciones = new IntencionesEnMemoria();
        sesiones = new SesionesEnMemoria();
        consultas = new ConsultasEnMemoria();
        configuracion = new ConfiguracionEnMemoria();
        eventos = new EventosEnMemoria();
        reloj = new RelojFijo();
        servicio = new ServicioConversacion(pln, intenciones, sesiones, consultas, configuracion, eventos,
                new SelectorRespuesta(RandomGenerator.getDefault()), reloj);
        intenciones.agregar("consulta_matricula", "académico", "registro", false,
                "Las fechas de matrícula están en el calendario académico.");
        sesion = servicio.iniciar("web").sesionId();
    }

    @Test
    void iniciaSesionAnonimaConFormatoEsperado() {
        assertTrue(sesion.matches("SES-2026-[A-Z0-9]{8}"), sesion);
        assertTrue(sesiones.porId(sesion).orElseThrow().estaActiva());
    }

    @Test
    void respondeCuandoLaConfianzaAlcanzaElUmbral() {
        pln.confianza = 0.60; // valor límite: con 60 % exacto sí responde
        RespuestaChat r = servicio.procesar(sesion, "¿Cuándo son las matrículas?");
        assertEquals(RespuestaChat.RESPUESTA, r.tipo());
        assertEquals("consulta_matricula", r.intent());
        assertNull(r.dependencia());
        assertEquals(1, eventos.contar(ConsultaResuelta.class));
        RegistroConsulta registro = consultas.datos.get(0);
        assertTrue(registro.resuelta());
        assertNotNull(registro.respuestaId());
    }

    @Test
    void escalaCuandoLaConfianzaEstaDebajoDelUmbral() {
        pln.confianza = 0.59; // valor límite: con 59 % escala (RF-05)
        RespuestaChat r = servicio.procesar(sesion, "¿Y eso cómo se paga?");
        assertEquals(RespuestaChat.ESCALAMIENTO, r.tipo());
        assertEquals("registro", r.dependencia().clave());
        assertTrue(r.texto().contains("Registro y Control Académico"));
        assertEquals("Confianza por debajo del umbral", consultas.datos.get(0).motivoEscalamiento());
        assertEquals(1, eventos.contar(ConsultaEscalada.class));
    }

    @Test
    void escalaALaDependenciaGeneralSiEstaFueraDelDominio() {
        pln.intencion = "nlu_fallback";
        pln.confianza = 0.95;
        RespuestaChat r = servicio.procesar(sesion, "¿Quién ganó el partido?");
        assertTrue(r.esEscalamiento());
        assertEquals("general", r.dependencia().clave());
        assertTrue(r.texto().startsWith(ServicioConversacion.INTRO_FUERA_DOMINIO));
        assertNull(r.intent());
        assertEquals("Fuera del dominio", consultas.datos.get(0).motivoEscalamiento());
    }

    @Test
    void escalaSiLaIntencionNoTieneRespuestasActivas() {
        Intencion it = intenciones.porNombre("consulta_matricula").orElseThrow();
        it.getRespuestas().forEach(r -> r.cambiarEstado(false, 1L, Fakes.AHORA));
        RespuestaChat r = servicio.procesar(sesion, "¿Cuándo son las matrículas?");
        assertTrue(r.esEscalamiento());
        assertEquals("Sin respuesta activa", consultas.datos.get(0).motivoEscalamiento());
    }

    @Test
    void elUmbralConfiguradoSeRespetaSinReiniciar() {
        configuracion.configuracion = new co.edu.uptc.asistente.dominio.modelo.Configuracion(0.80, 30,
                "Comunícate con {dependencia}.", true);
        pln.confianza = 0.75;
        assertTrue(servicio.procesar(sesion, "matrículas").esEscalamiento());
    }

    @Test
    void siElPlnSeCaeRegistraLaConsultaYPropagaElError() {
        pln.caido = true;
        assertThrows(PlnNoDisponibleException.class, () -> servicio.procesar(sesion, "hola"));
        assertEquals("Motor PLN no disponible", consultas.datos.get(0).motivoEscalamiento());
        assertFalse(consultas.datos.get(0).resuelta());
    }

    @Test
    void validaLongitudDeLaConsultaAntesDeLlamarAlPln() {
        assertThrows(ValidacionException.class, () -> servicio.procesar(sesion, "   "));
        assertThrows(ValidacionException.class, () -> servicio.procesar(sesion, "a".repeat(501)));
        servicio.procesar(sesion, "a".repeat(500)); // 500 exactos sí se aceptan
        assertEquals(1, pln.llamadas);
    }

    @Test
    void rechazaConsultasEnSesionesCerradasOInexistentes() {
        servicio.cerrar(sesion);
        assertThrows(ConflictoException.class, () -> servicio.procesar(sesion, "hola"));
        assertThrows(ConflictoException.class, () -> servicio.procesar("SES-NO-EXISTE", "hola"));
    }

    @Test
    void cuentaLasConsultasDeLaSesion() {
        servicio.procesar(sesion, "uno");
        servicio.procesar(sesion, "dos");
        assertEquals(2, sesiones.porId(sesion).orElseThrow().getTotalConsultas());
    }

    @Test
    void cierraSesionesInactivasDespuesDe30Minutos() {
        reloj.avanzarMinutos(29);
        assertEquals(0, servicio.cerrarInactivas());
        reloj.avanzarMinutos(2);
        assertEquals(1, servicio.cerrarInactivas());
        assertEquals("inactividad", sesiones.porId(sesion).orElseThrow().getCausaCierre());
        assertEquals(1, eventos.contar(SesionCerrada.class));
    }

    @Test
    void cerrarDosVecesNoDuplicaElEvento() {
        servicio.cerrar(sesion);
        servicio.cerrar(sesion);
        assertEquals(1, eventos.contar(SesionCerrada.class));
    }
}
