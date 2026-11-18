package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.dto.FiltroHistorial;
import co.edu.uptc.asistente.aplicacion.dto.Metricas;
import co.edu.uptc.asistente.aplicacion.dto.Metricas.ConteoIntencion;
import co.edu.uptc.asistente.aplicacion.dto.Metricas.PuntoSerie;
import co.edu.uptc.asistente.aplicacion.dto.Pagina;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.ConsultarMetricas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.MonitorDisponibilidad;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConsultas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioIntenciones;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Indicadores del dashboard e historial de consultas (UC-15). */
public class ServicioMetricas implements ConsultarMetricas {
    public static final double SLA_DISPONIBILIDAD = 0.95; // RNF-02
    private static final long LIMITE_RESPUESTA_MS = 3000;  // RNF-01
    private static final DateTimeFormatter FECHA_CSV = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RepositorioConsultas consultas;
    private final RepositorioIntenciones intenciones;
    private final MonitorDisponibilidad monitor;
    private final Clock reloj;
    private final ZoneId zona;

    public ServicioMetricas(RepositorioConsultas consultas, RepositorioIntenciones intenciones,
                            MonitorDisponibilidad monitor, Clock reloj, ZoneId zona) {
        this.consultas = consultas;
        this.intenciones = intenciones;
        this.monitor = monitor;
        this.reloj = reloj;
        this.zona = zona;
    }

    static int diasDeRango(String rango) {
        if ("hoy".equals(rango)) {
            return 1;
        }
        if ("30d".equals(rango)) {
            return 30;
        }
        if ("90d".equals(rango)) {
            return 90;
        }
        return 7;
    }

    private static String rangoValido(String rango) {
        return Set.of("hoy", "7d", "30d", "90d").contains(rango) ? rango : "7d";
    }

    @Override
    public Metricas calcular(String rangoPedido) {
        String rango = rangoValido(rangoPedido);
        int dias = diasDeRango(rango);
        Instant ahora = reloj.instant();
        ZonedDateTime inicioHoy = ahora.atZone(zona).toLocalDate().atStartOfDay(zona);
        Instant desde = inicioHoy.minusDays(dias - 1L).toInstant();
        Instant prevDesde = inicioHoy.minusDays(2L * dias - 1).toInstant();
        Instant prevHasta = ahora.atZone(zona).minusDays(dias).toInstant();

        List<RegistroConsulta> actuales = new ArrayList<>();
        List<RegistroConsulta> previas = new ArrayList<>();
        for (RegistroConsulta c : consultas.entre(prevDesde, ahora)) {
            if (!c.fechaHora().isBefore(desde)) {
                actuales.add(c);
            } else if (!c.fechaHora().isAfter(prevHasta)) {
                previas.add(c);
            }
        }

        long resueltas = actuales.stream().filter(RegistroConsulta::resuelta).count();
        double promedio = actuales.stream().mapToLong(RegistroConsulta::tiempoMs).average().orElse(0);
        double bajo3s = actuales.isEmpty() ? 0
                : actuales.stream().filter(c -> c.tiempoMs() <= LIMITE_RESPUESTA_MS).count() / (double) actuales.size();
        long sesiones = actuales.stream().map(RegistroConsulta::sesionId).distinct().count();
        boolean porHora = "hoy".equals(rango);

        return new Metricas(rango, porHora ? "hora" : "dia", actuales.size(), previas.size(), resueltas,
                actuales.size() - resueltas, tasa(actuales), tasa(previas), promedio, bajo3s, sesiones,
                monitor.disponibilidad(desde, ahora).orElse(1.0), SLA_DISPONIBILIDAD,
                serie(actuales, inicioHoy, dias, porHora, ahora), top(actuales));
    }

    private List<PuntoSerie> serie(List<RegistroConsulta> actuales, ZonedDateTime inicioHoy, int dias,
                                   boolean porHora, Instant ahora) {
        int n = porHora ? ahora.atZone(zona).getHour() + 1 : dias;
        ZonedDateTime origen = porHora ? inicioHoy : inicioHoy.minusDays(dias - 1L);
        long[] ok = new long[n];
        long[] esc = new long[n];
        for (RegistroConsulta c : actuales) {
            ZonedDateTime f = c.fechaHora().atZone(zona);
            int i = porHora ? f.getHour()
                    : (int) java.time.temporal.ChronoUnit.DAYS.between(origen.toLocalDate(), f.toLocalDate());
            if (i >= 0 && i < n) {
                if (c.resuelta()) {
                    ok[i]++;
                } else {
                    esc[i]++;
                }
            }
        }
        List<PuntoSerie> serie = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ZonedDateTime inicio = porHora ? origen.plusHours(i) : origen.plusDays(i);
            serie.add(new PuntoSerie(i, inicio.toInstant().toEpochMilli(), ok[i], esc[i]));
        }
        return serie;
    }

    private List<ConteoIntencion> top(List<RegistroConsulta> actuales) {
        Set<String> sociales = new HashSet<>();
        intenciones.listar().stream().filter(Intencion::isSocial).forEach(i -> sociales.add(i.getNombre()));
        Map<String, Long> conteo = new HashMap<>();
        actuales.stream()
                .filter(c -> c.resuelta() && c.intent() != null && !sociales.contains(c.intent()))
                .forEach(c -> conteo.merge(c.intent(), 1L, Long::sum));
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(e -> new ConteoIntencion(e.getKey(), e.getValue()))
                .toList();
    }

    private static double tasa(List<RegistroConsulta> lista) {
        return lista.isEmpty() ? 0 : lista.stream().filter(RegistroConsulta::resuelta).count() / (double) lista.size();
    }

    @Override
    public Pagina<RegistroConsulta> historial(FiltroHistorial f) {
        String termino = f.q() == null ? "" : f.q().strip().toLowerCase(Locale.ROOT);
        List<RegistroConsulta> lista = delRango(f.rango()).stream()
                .filter(c -> f.estado() == null || f.estado().isBlank()
                        || ("resuelta".equals(f.estado()) == c.resuelta()))
                .filter(c -> termino.isEmpty() || c.texto().toLowerCase(Locale.ROOT).contains(termino)
                        || (c.intent() != null && c.intent().contains(termino)))
                .toList();
        return Pagina.de(lista, f.page(), f.pageSize() <= 0 ? 15 : f.pageSize());
    }

    @Override
    public String exportarCsv(String rango) {
        StringBuilder sb = new StringBuilder(
                "fecha_hora;sesion;canal;consulta;intencion;categoria;confianza;estado;motivo_escalamiento;tiempo_ms\n");
        for (RegistroConsulta c : delRango(rango)) {
            sb.append(String.join(";",
                    FECHA_CSV.format(c.fechaHora().atZone(zona)),
                    celda(c.sesionId()),
                    celda(c.canal()),
                    celda(c.texto()),
                    celda(c.intent()),
                    celda(c.categoria()),
                    c.confianza() == null ? "" : String.format(Locale.ROOT, "%.2f", c.confianza()),
                    c.resuelta() ? "resuelta" : "escalada",
                    celda(c.motivoEscalamiento()),
                    String.valueOf(c.tiempoMs()))).append('\n');
        }
        return sb.toString();
    }

    private List<RegistroConsulta> delRango(String rango) {
        Instant ahora = reloj.instant();
        Instant desde = ahora.atZone(zona).toLocalDate().atStartOfDay(zona)
                .minusDays(diasDeRango(rangoValido(rango)) - 1L).toInstant();
        return consultas.entre(desde, ahora);
    }

    /** Escapa comillas y evita inyección de fórmulas al abrir el CSV en Excel. */
    static String celda(String valor) {
        if (valor == null) {
            return "";
        }
        String v = valor.replace("\r", " ").replace("\n", " ");
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /** Usado por las pruebas. */
    static Map<String, Long> conteoPorEstado(List<RegistroConsulta> lista) {
        return lista.stream().collect(Collectors.groupingBy(c -> c.resuelta() ? "resuelta" : "escalada",
                Collectors.counting()));
    }
}
