package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "configuracion")
public class ConfiguracionEntity {
    @Id
    private Integer id;

    @Column(name = "umbral_confianza", nullable = false, precision = 3, scale = 2)
    private BigDecimal umbralConfianza;

    @Column(name = "inactividad_sesion_min", nullable = false)
    private int inactividadSesionMin;

    @Column(name = "plantilla_escalamiento", nullable = false, length = 500)
    private String plantillaEscalamiento;

    @Column(name = "notificar_por_correo", nullable = false)
    private boolean notificarPorCorreo;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public BigDecimal getUmbralConfianza() { return umbralConfianza; }
    public void setUmbralConfianza(BigDecimal umbralConfianza) { this.umbralConfianza = umbralConfianza; }
    public int getInactividadSesionMin() { return inactividadSesionMin; }
    public void setInactividadSesionMin(int inactividadSesionMin) { this.inactividadSesionMin = inactividadSesionMin; }
    public String getPlantillaEscalamiento() { return plantillaEscalamiento; }
    public void setPlantillaEscalamiento(String plantillaEscalamiento) { this.plantillaEscalamiento = plantillaEscalamiento; }
    public boolean isNotificarPorCorreo() { return notificarPorCorreo; }
    public void setNotificarPorCorreo(boolean notificarPorCorreo) { this.notificarPorCorreo = notificarPorCorreo; }
}
