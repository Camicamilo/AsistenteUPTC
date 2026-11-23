package co.edu.uptc.asistente.aplicacion.puerto.entrada;

import co.edu.uptc.asistente.dominio.modelo.Configuracion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.RegistroAuditoria;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.util.List;

/** Parámetros del sistema y auditoría (UC-16). */
public interface ConfigurarSistema {
    Configuracion obtener();

    Configuracion actualizar(Configuracion nueva, Usuario actor);

    Configuracion restaurar(Usuario actor);

    Dependencia actualizarDependencia(String clave, Dependencia datos, Usuario actor);

    List<RegistroAuditoria> auditoria(int limite, Usuario actor);
}
