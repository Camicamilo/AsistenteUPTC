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

@Entity
@Table(name = "ejemplos_intencion")
public class EjemploEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ejemplo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intent_id")
    private IntencionEntity intencion;

    @Column(nullable = false, length = 500)
    private String texto;

    protected EjemploEntity() {
    }

    public EjemploEntity(IntencionEntity intencion, String texto) {
        this.intencion = intencion;
        this.texto = texto;
    }

    public Long getId() { return id; }
    public String getTexto() { return texto; }
}
