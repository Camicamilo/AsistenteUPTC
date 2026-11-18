package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auditoria")
public class AuditoriaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auditoria_id")
    private Long id;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false, length = 120)
    private String usuario;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(length = 500)
    private String detalle;

    protected AuditoriaEntity() {
    }

    public AuditoriaEntity(Instant fecha, String usuario, String accion, String detalle) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.accion = accion;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Instant getFecha() { return fecha; }
    public String getUsuario() { return usuario; }
    public String getAccion() { return accion; }
    public String getDetalle() { return detalle; }
}
