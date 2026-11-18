package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "respuestas")
public class RespuestaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "respuesta_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intent_id")
    private IntencionEntity intencion;

    @Column(name = "texto_respuesta", nullable = false, length = 1500)
    private String texto;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    public Long getId() { return id; }
    public IntencionEntity getIntencion() { return intencion; }
    public void setIntencion(IntencionEntity intencion) { this.intencion = intencion; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Instant fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public Long getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(Long actualizadoPor) { this.actualizadoPor = actualizadoPor; }
}
