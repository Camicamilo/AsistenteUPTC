package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.dto.NuevaSesion;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarSesiones;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.ProcesarConsulta;
import co.edu.uptc.asistente.aplicacion.puerto.salida.ClasificadorIntencion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.PublicadorEventos;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConfiguracion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConsultas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioIntenciones;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioSesiones;
import co.edu.uptc.asistente.dominio.evento.ConsultaEscalada;
import co.edu.uptc.asistente.dominio.evento.ConsultaResuelta;
import co.edu.uptc.asistente.dominio.evento.SesionCerrada;
import co.edu.uptc.asistente.dominio.evento.SesionIniciada;
import co.edu.uptc.asistente.dominio.excepcion.ConflictoException;
import co.edu.uptc.asistente.dominio.excepcion.PlnNoDisponibleException;
import co.edu.uptc.asistente.dominio.modelo.Configuracion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.MotivoEscalamiento;
import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;
import co.edu.uptc.asistente.dominio.modelo.Respuesta;
import co.edu.uptc.asistente.dominio.modelo.RespuestaChat;
import co.edu.uptc.asistente.dominio.modelo.ResultadoClasificacion;
import co.edu.uptc.asistente.dominio.modelo.SesionChat;
import co.edu.uptc.asistente.dominio.modelo.TemaRapido;
import co.edu.uptc.asistente.dominio.modelo.TextoConsulta;
import co.edu.uptc.asistente.dominio.servicio.PoliticaEscalamiento;
import co.edu.uptc.asistente.dominio.servicio.SelectorRespuesta;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Caso de uso Procesar Consulta (UC-05 a UC-11) y manejo de sesiones (UC-04, UC-09).
 * Es el núcleo del hexágono: no sabe nada de HTTP, de Rasa ni de SQL.
 */
public class ServicioConversacion implements ProcesarConsulta, GestionarSesiones {
    static final String INTRO_FUERA_DOMINIO = "Esa consulta parece estar fuera de los temas que manejo "
            + "(admisiones, matrículas, calendario académico, correo institucional y trámites). ";
    private static final char[] ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final ClasificadorIntencion clasificador;
    private final RepositorioIntenciones intenciones;
    private final RepositorioSesiones sesiones;
    private final RepositorioConsultas consultas;
    private final RepositorioConfiguracion configuracion;
    private final PublicadorEventos eventos;
    private final SelectorRespuesta selector;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    public ServicioConversacion(ClasificadorIntencion clasificador, RepositorioIntenciones intenciones,
                                RepositorioSesiones sesiones, RepositorioConsultas consultas,
                                RepositorioConfiguracion configuracion, PublicadorEventos eventos,
                                SelectorRespuesta selector, Clock reloj) {
        this.clasificador = clasificador;
        this.intenciones = intenciones;
        this.sesiones = sesiones;
        this.consultas = consultas;
        this.configuracion = configuracion;
        this.eventos = eventos;
        this.selector = selector;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ sesiones
    @Override
    public NuevaSesion iniciar(String canal) {
        Instant ahora = reloj.instant();
        SesionChat sesion = SesionChat.iniciar(nuevoId(), canal, ahora);
        sesiones.guardar(sesion);
        eventos.publicar(new SesionIniciada(sesion.getSesionId(), sesion.getCanal(), ahora));
        return new NuevaSesion(sesion.getSesionId(), ahora);
    }

    @Override
    public void cerrar(String sesionId) {
        sesiones.porId(sesionId).ifPresent(s -> cerrarSesion(s, SesionChat.CIERRE_USUARIO));
    }

    @Override
    public int cerrarInactivas() {
        int minutos = configuracion.obtener().inactividadSesionMin();
        Instant limite = reloj.instant().minus(minutos, ChronoUnit.MINUTES);
        List<SesionChat> inactivas = sesiones.abiertasSinActividadDesde(limite);
        inactivas.forEach(s -> cerrarSesion(s, SesionChat.CIERRE_INACTIVIDAD));
        return inactivas.size();
    }

    @Override
    public List<TemaRapido> temas() {
        return configuracion.temasRapidos();
    }

    private void cerrarSesion(SesionChat sesion, String causa) {
        Instant ahora = reloj.instant();
        if (sesion.cerrar(ahora, causa)) {
            sesiones.guardar(sesion);
            eventos.publicar(new SesionCerrada(sesion.getSesionId(), causa, sesion.getTotalConsultas(), ahora));
        }
    }

    private String nuevoId() {
        StringBuilder sb = new StringBuilder("SES-").append(Year.now(reloj).getValue()).append('-');
        for (int i = 0; i < 8; i++) {
            sb.append(ALFABETO[azar.nextInt(ALFABETO.length)]);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ consulta
    @Override
    public RespuestaChat procesar(String sesionId, String texto) {
        long inicio = System.nanoTime();
        TextoConsulta consulta = new TextoConsulta(texto);
        SesionChat sesion = sesiones.porId(String.valueOf(sesionId))
                .filter(SesionChat::estaActiva)
                .orElseThrow(() -> new ConflictoException("sesionId", "La sesión no existe o ya se cerró"));
        Configuracion cfg = configuracion.obtener();
        Instant ahora = reloj.instant();
        sesion.registrarConsulta(ahora);
        sesiones.guardar(sesion);

        ResultadoClasificacion resultado;
        try {
            resultado = clasificador.clasificar(consulta.valor());
        } catch (PlnNoDisponibleException e) {
            registrarEscalada(sesion, consulta, null, null, MotivoEscalamiento.PLN_NO_DISPONIBLE, "general",
                    milis(inicio), ahora);
            throw e;
        }

        Intencion intencion = resultado.esFueraDeDominio() ? null
                : intenciones.porNombre(resultado.intencion()).orElse(null);
        Optional<MotivoEscalamiento> motivo = PoliticaEscalamiento.evaluar(resultado, intencion, cfg.umbralConfianza());
        double confianza = resultado.confianza().redondeado();

        if (motivo.isEmpty()) {
            Respuesta respuesta = selector.elegir(intencion);
            long ms = milis(inicio);
            consultas.registrar(new RegistroConsulta(null, sesion.getSesionId(), consulta.valor(),
                    intencion.getNombre(), intencion.getCategoria(), confianza, true, null, respuesta.getId(), ms,
                    sesion.getCanal(), ahora));
            eventos.publicar(new ConsultaResuelta(sesion.getSesionId(), consulta.valor(), intencion.getNombre(),
                    confianza, respuesta.getId(), ms, ahora));
            return new RespuestaChat(RespuestaChat.RESPUESTA, respuesta.getTexto(), intencion.getNombre(),
                    intencion.getCategoria(), confianza, resultado.entidades(), null, ms);
        }

        String claveDependencia = intencion == null ? "general" : intencion.getDependencia();
        Dependencia dependencia = dependencia(claveDependencia);
        String mensaje = (motivo.get() == MotivoEscalamiento.FUERA_DEL_DOMINIO ? INTRO_FUERA_DOMINIO : "")
                + cfg.mensajeEscalamiento(dependencia);
        long ms = milis(inicio);
        registrarEscalada(sesion, consulta, intencion, confianza, motivo.get(),
                dependencia == null ? claveDependencia : dependencia.clave(), ms, ahora);
        return new RespuestaChat(RespuestaChat.ESCALAMIENTO, mensaje,
                intencion == null ? null : intencion.getNombre(),
                intencion == null ? null : intencion.getCategoria(),
                confianza, resultado.entidades(), dependencia, ms);
    }

    private void registrarEscalada(SesionChat sesion, TextoConsulta consulta, Intencion intencion, Double confianza,
                                   MotivoEscalamiento motivo, String dependencia, long ms, Instant ahora) {
        String nombre = intencion == null ? null : intencion.getNombre();
        String categoria = intencion == null ? null : intencion.getCategoria();
        consultas.registrar(new RegistroConsulta(null, sesion.getSesionId(), consulta.valor(), nombre, categoria,
                confianza, false, motivo.etiqueta(), null, ms, sesion.getCanal(), ahora));
        eventos.publicar(new ConsultaEscalada(sesion.getSesionId(), consulta.valor(), nombre, categoria, confianza,
                motivo.etiqueta(), dependencia, ahora));
    }

    private Dependencia dependencia(String clave) {
        return configuracion.dependencia(clave).or(() -> configuracion.dependencia("general")).orElse(null);
    }

    private static long milis(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
