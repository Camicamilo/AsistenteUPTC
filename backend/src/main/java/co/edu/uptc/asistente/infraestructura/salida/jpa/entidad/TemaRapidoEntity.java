package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "temas_rapidos")
public class TemaRapidoEntity {
    @Id
    @Column(name = "tema_id", length = 30)
    private String id;

    @Column(nullable = false, length = 60)
    private String etiqueta;

    @Column(nullable = false, length = 200)
    private String consulta;

    @Column(nullable = false)
    private int orden;

    public String getId() { return id; }
    public String getEtiqueta() { return etiqueta; }
    public String getConsulta() { return consulta; }
    public int getOrden() { return orden; }
}
