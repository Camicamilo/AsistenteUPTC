package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.dto.NuevaIntencion;
import co.edu.uptc.asistente.aplicacion.dto.Pagina;
import co.edu.uptc.asistente.aplicacion.dto.ResumenIntencion;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarConocimiento;
import co.edu.uptc.asistente.aplicacion.puerto.salida.AlmacenEventos;
import co.edu.uptc.asistente.aplicacion.puerto.salida.EntrenadorModelo;
import co.edu.uptc.asistente.aplicacion.puerto.salida.PublicadorEventos;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioAuditoria;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConfiguracion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioIntenciones;
import co.edu.uptc.asistente.dominio.evento.ConocimientoActualizado;
import co.edu.uptc.asistente.dominio.excepcion.ConflictoException;
import co.edu.uptc.asistente.dominio.excepcion.NoEncontradoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.EventoAlmacenado;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.Respuesta;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Base de conocimiento: CRUD de intenciones y respuestas, auditoría, eventos y reentrenamiento. */
public class ServicioConocimiento implements GestionarConocimiento {
    static final int MIN_EJEMPLOS = 3;

    private final RepositorioIntenciones intenciones;
    private final RepositorioConfiguracion configuracion;
    private final PublicadorEventos eventos;
    private final AlmacenEventos almacen;
    private final EntrenadorModelo entrenador;
    private final RepositorioAuditoria auditoria;
    private final Clock reloj;

    public ServicioConocimiento(RepositorioIntenciones intenciones, RepositorioConfiguracion configuracion,
                                PublicadorEventos eventos, AlmacenEventos almacen, EntrenadorModelo entrenador,
                                RepositorioAuditoria auditoria, Clock reloj) {
        this.intenciones = intenciones;
        this.configuracion = configuracion;
        this.eventos = eventos;
        this.almacen = almacen;
        this.entrenador = entrenador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Override
    public Pagina<ResumenIntencion> listar(String q, String categoria, int page, int pageSize) {
        String termino = q == null ? "" : q.strip().toLowerCase(Locale.ROOT);
        List<ResumenIntencion> lista = intenciones.listar().stream()
                .filter(it -> categoria == null || categoria.isBlank() || it.getCategoria().equals(categoria))
                .filter(it -> termino.isEmpty()
                        || it.getNombre().toLowerCase(Locale.ROOT).contains(termino)
                        || it.getDescripcion().toLowerCase(Locale.ROOT).contains(termino)
                        || it.getRespuestas().stream()
                        .anyMatch(r -> r.getTexto().toLowerCase(Locale.ROOT).contains(termino)))
                .sorted(Comparator.comparing(Intencion::getNombre))
                .map(ResumenIntencion::de)
                .toList();
        return Pagina.de(lista, page, pageSize);
    }

    @Override
    public Intencion obtener(long id) {
        return intenciones.porId(id).orElseThrow(() -> new NoEncontradoException("La intención no existe"));
    }

    @Override
    public Intencion crear(NuevaIntencion datos, Usuario actor) {
        Autorizacion.exigirSesion(actor);
        String nombre = Intencion.validarNombre(datos.nombre());
        if (intenciones.existeNombre(nombre)) {
            throw new ConflictoException("nombre", "Ya existe una intención con ese nombre");
        }
        String dependencia = validarDependencia(datos.dependencia());
        String texto = Respuesta.validarTexto(datos.respuesta());
        List<String> ejemplos = limpiarEjemplos(datos.ejemplos());
        if (ejemplos.size() < MIN_EJEMPLOS) {
            throw new ValidacionException("ejemplos",
                    "Escribe al menos " + MIN_EJEMPLOS + " frases de ejemplo (una por línea) para entrenar al asistente");
        }
        Instant ahora = reloj.instant();
        Intencion nueva = new Intencion(null, nombre, datos.categoria(), datos.descripcion(), dependencia, false,
                ejemplos, List.of(Respuesta.nueva(texto, actor.getId(), ahora)));
        Intencion guardada = intenciones.guardar(nueva);
        auditoria.registrar(actor.getCorreo(), "Crear intención", nombre);
        publicar(guardada, "IntencionCreada", guardada.getRespuestas().get(0).getId(), texto, actor, ahora, true);
        return guardada;
    }

    @Override
    public Intencion actualizar(long id, String categoria, String descripcion, String dependencia, Usuario actor) {
        Autorizacion.exigirSesion(actor);
        Intencion it = obtener(id);
        it.actualizarDatos(categoria, descripcion,
                dependencia == null || dependencia.isBlank() ? null : validarDependencia(dependencia));
        Intencion guardada = intenciones.guardar(it);
        auditoria.registrar(actor.getCorreo(), "Editar intención", it.getNombre());
        publicar(guardada, "IntencionEditada", null, null, actor, reloj.instant(), false);
        return guardada;
    }

    @Override
    public Intencion agregarRespuesta(long id, String texto, Usuario actor) {
        Autorizacion.exigirSesion(actor);
        Intencion it = obtener(id);
        Instant ahora = reloj.instant();
        Respuesta nueva = it.agregarRespuesta(texto, actor.getId(), ahora);
        Intencion guardada = intenciones.guardar(it);
        Long respuestaId = guardada.getRespuestas().stream().map(Respuesta::getId)
                .max(Long::compare).orElse(null);
        auditoria.registrar(actor.getCorreo(), "Agregar respuesta", it.getNombre());
        publicar(guardada, "RespuestaCreada", respuestaId, nueva.getTexto(), actor, ahora, false);
        return guardada;
    }

    @Override
    public Intencion actualizarRespuesta(long id, long respuestaId, String texto, Boolean activa, Usuario actor) {
        Autorizacion.exigirSesion(actor);
        if (texto == null && activa == null) {
            throw new ValidacionException("No hay cambios para guardar");
        }
        Intencion it = obtener(id);
        Respuesta r = it.respuesta(respuestaId);
        Instant ahora = reloj.instant();
        if (texto != null) {
            r.editar(texto, actor.getId(), ahora);
        }
        if (activa != null) {
            r.cambiarEstado(activa, actor.getId(), ahora);
        }
        Intencion guardada = intenciones.guardar(it);
        String accion = activa == null ? "Editar respuesta" : activa ? "Activar respuesta" : "Desactivar respuesta";
        String tipo = activa == null ? "RespuestaEditada" : activa ? "RespuestaActivada" : "RespuestaDesactivada";
        auditoria.registrar(actor.getCorreo(), accion, it.getNombre() + " #" + respuestaId);
        publicar(guardada, tipo, respuestaId, texto, actor, ahora, false);
        return guardada;
    }

    @Override
    public List<EventoAlmacenado> historial(long id) {
        Intencion it = obtener(id);
        return almacen.delAgregado("INT-" + it.getId());
    }

    @Override
    public List<Dependencia> dependencias() {
        return configuracion.dependencias();
    }

    @Override
    public void sincronizarModelo() {
        Map<String, List<String>> datos = new LinkedHashMap<>();
        intenciones.listar().stream()
                .filter(it -> !it.getEjemplos().isEmpty())
                .forEach(it -> datos.put(it.getNombre(), it.getEjemplos()));
        if (datos.size() >= 2) {
            entrenador.entrenar(datos);
        }
    }

    private void publicar(Intencion it, String tipo, Long respuestaId, String texto, Usuario actor, Instant ahora,
                          boolean reentrenar) {
        eventos.publicar(new ConocimientoActualizado(it.getId(), it.getNombre(), tipo, respuestaId, texto,
                actor.getId(), ahora, reentrenar));
    }

    private String validarDependencia(String clave) {
        String c = clave == null || clave.isBlank() ? "general" : clave.strip();
        return configuracion.dependencia(c).map(Dependencia::clave)
                .orElseThrow(() -> new ValidacionException("dependencia", "La dependencia no existe"));
    }

    private static List<String> limpiarEjemplos(List<String> ejemplos) {
        LinkedHashSet<String> limpios = new LinkedHashSet<>();
        if (ejemplos != null) {
            for (String e : ejemplos) {
                String t = e == null ? "" : e.strip();
                if (!t.isEmpty()) {
                    if (t.length() > 500) {
                        throw new ValidacionException("ejemplos", "Cada ejemplo puede tener máximo 500 caracteres");
                    }
                    limpios.add(t);
                }
            }
        }
        return List.copyOf(limpios);
    }
}
