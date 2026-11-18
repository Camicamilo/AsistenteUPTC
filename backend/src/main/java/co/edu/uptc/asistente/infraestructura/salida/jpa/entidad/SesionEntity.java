package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sesiones_chat")
public class SesionEntity {
    @Id
    @Column(name = "sesion_id", length = 100)
    private String id;

    @Column(nullable = false, length = 30)
    private String canal;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "ultima_actividad", nullable = false)
    private Instant ultimaActividad;

    @Column(name = "fecha_fin")
    private Instant fechaFin;

    @Column(name = "causa_cierre", length = 30)
    private String causaCierre;

    @Column(name = "total_consultas", nullable = false)
    private int totalConsultas;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public Instant getUltimaActividad() { return ultimaActividad; }
    public void setUltimaActividad(Instant ultimaActividad) { this.ultimaActividad = ultimaActividad; }
    public Instant getFechaFin() { return fechaFin; }
    public void setFechaFin(Instant fechaFin) { this.fechaFin = fechaFin; }
    public String getCausaCierre() { return causaCierre; }
    public void setCausaCierre(String causaCierre) { this.causaCierre = causaCierre; }
    public int getTotalConsultas() { return totalConsultas; }
    public void setTotalConsultas(int totalConsultas) { this.totalConsultas = totalConsultas; }
}
