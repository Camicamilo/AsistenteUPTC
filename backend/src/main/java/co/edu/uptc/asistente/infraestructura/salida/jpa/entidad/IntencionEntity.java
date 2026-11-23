package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "intenciones")
public class IntencionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "intent_id")
    private Long id;

    @Column(name = "nombre_intent", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(nullable = false, length = 30)
    private String dependencia;

    @Column(nullable = false)
    private boolean social;

    @OneToMany(mappedBy = "intencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 64)
    private List<RespuestaEntity> respuestas = new ArrayList<>();

    @OneToMany(mappedBy = "intencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 64)
    private List<EjemploEntity> ejemplos = new ArrayList<>();

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }
    public boolean isSocial() { return social; }
    public void setSocial(boolean social) { this.social = social; }
    public List<RespuestaEntity> getRespuestas() { return respuestas; }
    public List<EjemploEntity> getEjemplos() { return ejemplos; }
}
