package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chequeos_disponibilidad")
public class ChequeoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chequeo_id")
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(name = "pln_ok", nullable = false)
    private boolean plnOk;

    @Column(name = "bd_ok", nullable = false)
    private boolean bdOk;

    protected ChequeoEntity() {
    }

    public ChequeoEntity(Instant fechaHora, boolean plnOk, boolean bdOk) {
        this.fechaHora = fechaHora;
        this.plnOk = plnOk;
        this.bdOk = bdOk;
    }

    public Long getId() { return id; }
    public Instant getFechaHora() { return fechaHora; }
    public boolean isPlnOk() { return plnOk; }
    public boolean isBdOk() { return bdOk; }
}
