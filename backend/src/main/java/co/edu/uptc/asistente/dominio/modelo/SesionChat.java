package co.edu.uptc.asistente.dominio.modelo;

import java.time.Duration;
import java.time.Instant;

/**
 * Agregado raíz de una conversación. Es anónima: se identifica con un código y nunca
 * guarda datos personales del estudiante (RD-02, Ley 1581).
 */
public class SesionChat {
    public static final String CIERRE_USUARIO = "usuario";
    public static final String CIERRE_INACTIVIDAD = "inactividad";

    private final String sesionId;
    private final String canal;
    private final Instant fechaInicio;
    private Instant ultimaActividad;
    private Instant fechaFin;
    private String causaCierre;
    private int totalConsultas;

    public SesionChat(String sesionId, String canal, Instant fechaInicio, Instant ultimaActividad,
                      Instant fechaFin, String causaCierre, int totalConsultas) {
        this.sesionId = sesionId;
        this.canal = normalizarCanal(canal);
        this.fechaInicio = fechaInicio;
        this.ultimaActividad = ultimaActividad == null ? fechaInicio : ultimaActividad;
        this.fechaFin = fechaFin;
        this.causaCierre = causaCierre;
        this.totalConsultas = totalConsultas;
    }

    public static SesionChat iniciar(String sesionId, String canal, Instant ahora) {
        return new SesionChat(sesionId, canal, ahora, ahora, null, null, 0);
    }

    public static String normalizarCanal(String canal) {
        if (canal == null) {
            return "web";
        }
        String c = canal.strip().toLowerCase();
        return c.equals("movil") || c.equals("móvil") ? "móvil" : "web";
    }

    public boolean estaActiva() {
        return fechaFin == null;
    }

    public void registrarConsulta(Instant ahora) {
        totalConsultas++;
        ultimaActividad = ahora;
    }

    public boolean inactivaDesde(Instant ahora, int minutos) {
        return estaActiva() && Duration.between(ultimaActividad, ahora).toMinutes() >= minutos;
    }

    /** Cierra la sesión. Devuelve false si ya estaba cerrada (operación idempotente). */
    public boolean cerrar(Instant ahora, String causa) {
        if (!estaActiva()) {
            return false;
        }
        fechaFin = ahora;
        causaCierre = causa;
        return true;
    }

    public String getSesionId() { return sesionId; }
    public String getCanal() { return canal; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getUltimaActividad() { return ultimaActividad; }
    public Instant getFechaFin() { return fechaFin; }
    public String getCausaCierre() { return causaCierre; }
    public int getTotalConsultas() { return totalConsultas; }
}
