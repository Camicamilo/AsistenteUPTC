package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "consultas")
public class ConsultaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consulta_id")
    private Long id;

    @Column(name = "sesion_id", nullable = false, length = 100)
    private String sesionId;

    @Column(name = "texto_original", nullable = false, length = 500)
    private String texto;

    @Column(name = "intent_detectado", length = 80)
    private String intent;

    @Column(length = 50)
    private String categoria;

    @Column(precision = 4, scale = 3)
    private BigDecimal confianza;

    @Column(nullable = false)
    private boolean resuelta;

    @Column(name = "motivo_escalamiento", length = 60)
    private String motivoEscalamiento;

    @Column(name = "respuesta_id")
    private Long respuestaId;

    @Column(name = "tiempo_ms", nullable = false)
    private int tiempoMs;

    @Column(nullable = false, length = 30)
    private String canal;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    public Long getId() { return id; }
    public String getSesionId() { return sesionId; }
    public void setSesionId(String sesionId) { this.sesionId = sesionId; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public BigDecimal getConfianza() { return confianza; }
    public void setConfianza(BigDecimal confianza) { this.confianza = confianza; }
    public boolean isResuelta() { return resuelta; }
    public void setResuelta(boolean resuelta) { this.resuelta = resuelta; }
    public String getMotivoEscalamiento() { return motivoEscalamiento; }
    public void setMotivoEscalamiento(String motivoEscalamiento) { this.motivoEscalamiento = motivoEscalamiento; }
    public Long getRespuestaId() { return respuestaId; }
    public void setRespuestaId(Long respuestaId) { this.respuestaId = respuestaId; }
    public int getTiempoMs() { return tiempoMs; }
    public void setTiempoMs(int tiempoMs) { this.tiempoMs = tiempoMs; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public Instant getFechaHora() { return fechaHora; }
    public void setFechaHora(Instant fechaHora) { this.fechaHora = fechaHora; }
}
