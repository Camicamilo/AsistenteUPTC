package co.edu.uptc.asistente.dominio.modelo;

import co.edu.uptc.asistente.dominio.excepcion.NoEncontradoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Agregado raíz de la base de conocimiento: una intención con sus respuestas y sus
 * ejemplos de entrenamiento. Regla: toda respuesta pertenece a una intención y solo
 * se entregan las activas (RF-03, RD-03).
 */
public class Intencion {
    public static final Set<String> CATEGORIAS = Set.of("académico", "administrativo", "técnico");
    private static final Pattern NOMBRE = Pattern.compile("^[a-z][a-z0-9_]{2,79}$");

    private Long id;
    private final String nombre;
    private String categoria;
    private String descripcion;
    private String dependencia;
    private final boolean social;
    private final List<String> ejemplos;
    private final List<Respuesta> respuestas;

    public Intencion(Long id, String nombre, String categoria, String descripcion, String dependencia,
                     boolean social, List<String> ejemplos, List<Respuesta> respuestas) {
        this.id = id;
        this.nombre = validarNombre(nombre);
        this.categoria = validarCategoria(categoria);
        this.descripcion = descripcion == null ? "" : descripcion.strip();
        this.dependencia = dependencia == null || dependencia.isBlank() ? "general" : dependencia;
        this.social = social;
        this.ejemplos = new ArrayList<>(ejemplos == null ? List.of() : ejemplos);
        this.respuestas = new ArrayList<>(respuestas == null ? List.of() : respuestas);
    }

    public static String validarNombre(String nombre) {
        String n = nombre == null ? "" : nombre.strip();
        if (!NOMBRE.matcher(n).matches()) {
            throw new ValidacionException("nombre", "Usa minúsculas, números y guion bajo (ej.: consulta_matricula)");
        }
        return n;
    }

    private static String validarCategoria(String categoria) {
        if (categoria == null || !CATEGORIAS.contains(categoria)) {
            throw new ValidacionException("categoria", "Selecciona una categoría");
        }
        return categoria;
    }

    public List<Respuesta> respuestasActivas() {
        return respuestas.stream().filter(Respuesta::isActiva).toList();
    }

    public boolean tieneRespuestaActiva() {
        return respuestas.stream().anyMatch(Respuesta::isActiva);
    }

    public Respuesta agregarRespuesta(String texto, Long usuarioId, Instant ahora) {
        Respuesta r = Respuesta.nueva(texto, usuarioId, ahora);
        respuestas.add(r);
        return r;
    }

    public Respuesta respuesta(long respuestaId) {
        return buscarRespuesta(respuestaId).orElseThrow(() -> new NoEncontradoException("La respuesta no existe"));
    }

    public Optional<Respuesta> buscarRespuesta(long respuestaId) {
        return respuestas.stream().filter(r -> r.getId() != null && r.getId() == respuestaId).findFirst();
    }

    public void actualizarDatos(String categoria, String descripcion, String dependencia) {
        if (categoria != null && !categoria.isBlank()) {
            this.categoria = validarCategoria(categoria);
        }
        if (descripcion != null) {
            this.descripcion = descripcion.strip();
        }
        if (dependencia != null && !dependencia.isBlank()) {
            this.dependencia = dependencia;
        }
    }

    public Instant ultimaActualizacion() {
        return respuestas.stream().map(Respuesta::getFechaActualizacion)
                .filter(f -> f != null).max(Instant::compareTo).orElse(null);
    }

    public Long getId() { return id; }
    public void asignarId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public String getDependencia() { return dependencia; }
    public boolean isSocial() { return social; }
    public List<String> getEjemplos() { return Collections.unmodifiableList(ejemplos); }
    public List<Respuesta> getRespuestas() { return Collections.unmodifiableList(respuestas); }
}
