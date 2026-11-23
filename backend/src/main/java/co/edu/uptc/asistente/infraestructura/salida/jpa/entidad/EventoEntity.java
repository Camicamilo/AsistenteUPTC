package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila del almacén de eventos. Inmutable: no tiene setters después de crearse. */
@Entity
@Table(name = "eventos")
public class EventoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evento_id")
    private Long id;

    @Column(name = "agregado_id", nullable = false, length = 100)
    private String agregadoId;

    @Column(name = "tipo_agregado", nullable = false, length = 30)
    private String tipoAgregado;

    @Column(nullable = false)
    private int version;

    @Column(name = "tipo_evento", nullable = false, length = 60)
    private String tipoEvento;

    @Column(nullable = false, length = 4000)
    private String datos;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(name = "usuario_id")
    private Long usuarioId;

    protected EventoEntity() {
    }

    public EventoEntity(String agregadoId, String tipoAgregado, int version, String tipoEvento, String datos,
                        Instant fechaHora, Long usuarioId) {
        this.agregadoId = agregadoId;
        this.tipoAgregado = tipoAgregado;
        this.version = version;
        this.tipoEvento = tipoEvento;
        this.datos = datos;
        this.fechaHora = fechaHora;
        this.usuarioId = usuarioId;
    }

    public Long getId() { return id; }
    public String getAgregadoId() { return agregadoId; }
    public String getTipoAgregado() { return tipoAgregado; }
    public int getVersion() { return version; }
    public String getTipoEvento() { return tipoEvento; }
    public String getDatos() { return datos; }
    public Instant getFechaHora() { return fechaHora; }
    public Long getUsuarioId() { return usuarioId; }
}
