package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.puerto.salida.AlmacenEventos;
import co.edu.uptc.asistente.aplicacion.puerto.salida.ClasificadorIntencion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uptc.asistente.aplicacion.puerto.salida.EntrenadorModelo;
import co.edu.uptc.asistente.aplicacion.puerto.salida.MonitorDisponibilidad;
import co.edu.uptc.asistente.aplicacion.puerto.salida.NotificadorEscalamiento;
import co.edu.uptc.asistente.aplicacion.puerto.salida.PublicadorEventos;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioAuditoria;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConfiguracion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConsultas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioIntenciones;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioSesiones;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uptc.asistente.dominio.evento.ConsultaEscalada;
import co.edu.uptc.asistente.dominio.evento.EventoDominio;
import co.edu.uptc.asistente.dominio.excepcion.PlnNoDisponibleException;
import co.edu.uptc.asistente.dominio.modelo.Configuracion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.EventoAlmacenado;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.NivelConfianza;
import co.edu.uptc.asistente.dominio.modelo.RegistroAuditoria;
import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;
import co.edu.uptc.asistente.dominio.modelo.Respuesta;
import co.edu.uptc.asistente.dominio.modelo.ResultadoClasificacion;
import co.edu.uptc.asistente.dominio.modelo.SesionChat;
import co.edu.uptc.asistente.dominio.modelo.TemaRapido;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptadores falsos en memoria. Gracias a la arquitectura hexagonal el núcleo se prueba sin
 * levantar Spring, la base de datos ni el motor PLN (8.7.5: capacidad de prueba).
 */
final class Fakes {
    private Fakes() {
    }

    static final Instant AHORA = Instant.parse("2026-11-18T15:00:00Z");

    /** Reloj que se puede adelantar en las pruebas. */
    static final class RelojFijo extends Clock {
        Instant instante = AHORA;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }

        void avanzarMinutos(long m) {
            instante = instante.plusSeconds(m * 60);
        }
    }

    /** Motor PLN falso: devuelve lo que la prueba le indique. */
    static final class ClasificadorFalso implements ClasificadorIntencion {
        String intencion = "consulta_matricula";
        double confianza = 0.91;
        boolean caido;
        int llamadas;

        @Override
        public ResultadoClasificacion clasificar(String texto) {
            llamadas++;
            if (caido) {
                throw new PlnNoDisponibleException("caído", null);
            }
            return new ResultadoClasificacion(intencion, NivelConfianza.de(confianza), List.of());
        }
    }

    static final class EntrenadorFalso implements EntrenadorModelo {
        Map<String, List<String>> ultimo;

        @Override
        public void entrenar(Map<String, List<String>> datos) {
            ultimo = datos;
        }
    }

    static final class IntencionesEnMemoria implements RepositorioIntenciones {
        final Map<Long, Intencion> datos = new HashMap<>();
        private final AtomicLong secuencia = new AtomicLong(1);
        private final AtomicLong secuenciaRespuestas = new AtomicLong(100);

        @Override
        public List<Intencion> listar() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public Optional<Intencion> porId(long id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public Optional<Intencion> porNombre(String nombre) {
            return datos.values().stream().filter(i -> i.getNombre().equals(nombre)).findFirst();
        }

        @Override
        public boolean existeNombre(String nombre) {
            return porNombre(nombre).isPresent();
        }

        @Override
        public Intencion guardar(Intencion it) {
            if (it.getId() == null) {
                it.asignarId(secuencia.getAndIncrement());
            }
            it.getRespuestas().stream().filter(r -> r.getId() == null)
                    .forEach(r -> r.asignarId(secuenciaRespuestas.getAndIncrement()));
            datos.put(it.getId(), it);
            return it;
        }

        Intencion agregar(String nombre, String categoria, String dependencia, boolean social, String... respuestas) {
            List<Respuesta> rs = new ArrayList<>();
            for (String r : respuestas) {
                rs.add(Respuesta.nueva(r, null, AHORA));
            }
            return guardar(new Intencion(null, nombre, categoria, "", dependencia, social,
                    List.of("ejemplo uno", "ejemplo dos", "ejemplo tres"), rs));
        }
    }

    static final class SesionesEnMemoria implements RepositorioSesiones {
        final Map<String, SesionChat> datos = new HashMap<>();

        @Override
        public void guardar(SesionChat s) {
            datos.put(s.getSesionId(), s);
        }

        @Override
        public Optional<SesionChat> porId(String id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public List<SesionChat> abiertasSinActividadDesde(Instant limite) {
            return datos.values().stream().filter(SesionChat::estaActiva)
                    .filter(s -> s.getUltimaActividad().isBefore(limite)).toList();
        }
    }

    static final class ConsultasEnMemoria implements RepositorioConsultas {
        final List<RegistroConsulta> datos = new ArrayList<>();

        @Override
        public void registrar(RegistroConsulta r) {
            datos.add(r);
        }

        @Override
        public List<RegistroConsulta> entre(Instant desde, Instant hasta) {
            return datos.stream().filter(c -> !c.fechaHora().isBefore(desde) && !c.fechaHora().isAfter(hasta))
                    .sorted(Comparator.comparing(RegistroConsulta::fechaHora).reversed()).toList();
        }
    }

    static final class ConfiguracionEnMemoria implements RepositorioConfiguracion {
        Configuracion configuracion = Configuracion.POR_DEFECTO;
        final Map<String, Dependencia> dependencias = new HashMap<>();

        ConfiguracionEnMemoria() {
            dependencias.put("general", new Dependencia("general", "Atención al Ciudadano", "a@uptc.edu.co", "", "", ""));
            dependencias.put("registro", new Dependencia("registro", "Registro y Control Académico", "r@uptc.edu.co",
                    "", "", ""));
        }

        @Override
        public Configuracion obtener() {
            return configuracion;
        }

        @Override
        public void guardar(Configuracion c) {
            configuracion = c;
        }

        @Override
        public List<Dependencia> dependencias() {
            return new ArrayList<>(dependencias.values());
        }

        @Override
        public Optional<Dependencia> dependencia(String clave) {
            return Optional.ofNullable(dependencias.get(clave));
        }

        @Override
        public void guardarDependencia(Dependencia d) {
            dependencias.put(d.clave(), d);
        }

        @Override
        public List<TemaRapido> temasRapidos() {
            return List.of(new TemaRapido("matriculas", "Matrículas", "¿Cuándo son las matrículas?"));
        }
    }

    static final class EventosEnMemoria implements PublicadorEventos, AlmacenEventos {
        final List<EventoDominio> publicados = new ArrayList<>();

        @Override
        public void publicar(EventoDominio evento) {
            publicados.add(evento);
        }

        @Override
        public void agregar(EventoDominio evento) {
            publicados.add(evento);
        }

        @Override
        public List<EventoAlmacenado> delAgregado(String agregadoId) {
            List<EventoAlmacenado> r = new ArrayList<>();
            int v = 1;
            for (EventoDominio e : publicados) {
                if (e.agregadoId().equals(agregadoId)) {
                    r.add(new EventoAlmacenado((long) v, agregadoId, e.tipoAgregado(), v++, e.tipoEvento(), "{}",
                            e.fechaHora(), e.usuarioId()));
                }
            }
            return r;
        }

        long contar(Class<?> tipo) {
            return publicados.stream().filter(tipo::isInstance).count();
        }
    }

    static final class AuditoriaEnMemoria implements RepositorioAuditoria {
        final List<RegistroAuditoria> datos = new ArrayList<>();

        @Override
        public void registrar(String usuario, String accion, String detalle) {
            datos.add(new RegistroAuditoria((long) datos.size() + 1, AHORA, usuario, accion, detalle));
        }

        @Override
        public List<RegistroAuditoria> ultimos(int limite) {
            return datos.stream().limit(limite).toList();
        }
    }

    static final class UsuariosEnMemoria implements RepositorioUsuarios {
        final Map<Long, Usuario> datos = new HashMap<>();
        private final AtomicLong secuencia = new AtomicLong(1);

        @Override
        public Optional<Usuario> porId(long id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public Optional<Usuario> porCorreo(String correo) {
            return datos.values().stream().filter(u -> u.getCorreo().equals(correo)).findFirst();
        }

        @Override
        public List<Usuario> listar() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public Usuario guardar(Usuario u) {
            if (u.getId() == null) {
                u.asignarId(secuencia.getAndIncrement());
            }
            datos.put(u.getId(), u);
            return u;
        }
    }

    /** "Hash" reversible solo para pruebas. */
    static final class CodificadorFalso implements CodificadorContrasenas {
        @Override
        public String codificar(String contrasena) {
            return "hash:" + contrasena;
        }

        @Override
        public boolean coincide(String contrasena, String hash) {
            return ("hash:" + contrasena).equals(hash);
        }
    }

    static final class TokensFalsos implements EmisorTokens {
        @Override
        public String emitir(Usuario u) {
            return "token-" + u.getId();
        }
    }

    static final class NotificadorFalso implements NotificadorEscalamiento {
        final List<String> enviados = new ArrayList<>();

        @Override
        public void notificar(ConsultaEscalada evento, Dependencia dependencia) {
            enviados.add(dependencia.clave());
        }

        @Override
        public void enviarCredenciales(String correo, String nombre, String contrasenaTemporal) {
            enviados.add(correo);
        }
    }

    static final class MonitorFalso implements MonitorDisponibilidad {
        @Override
        public OptionalDouble disponibilidad(Instant desde, Instant hasta) {
            return OptionalDouble.of(0.99);
        }
    }
}
