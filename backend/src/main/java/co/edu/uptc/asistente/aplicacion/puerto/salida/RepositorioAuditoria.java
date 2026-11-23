package co.edu.uptc.asistente.aplicacion.puerto.salida;

import co.edu.uptc.asistente.dominio.modelo.RegistroAuditoria;

import java.util.List;

public interface RepositorioAuditoria {
    void registrar(String usuario, String accion, String detalle);

    List<RegistroAuditoria> ultimos(int limite);
}
