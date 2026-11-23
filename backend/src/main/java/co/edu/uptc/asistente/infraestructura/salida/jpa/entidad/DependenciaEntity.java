package co.edu.uptc.asistente.infraestructura.salida.jpa.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dependencias")
public class DependenciaEntity {
    @Id
    @Column(length = 30)
    private String clave;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 120)
    private String correo;

    @Column(length = 40)
    private String telefono;

    @Column(length = 300)
    private String enlace;

    @Column(length = 120)
    private String horario;

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEnlace() { return enlace; }
    public void setEnlace(String enlace) { this.enlace = enlace; }
    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }
}
