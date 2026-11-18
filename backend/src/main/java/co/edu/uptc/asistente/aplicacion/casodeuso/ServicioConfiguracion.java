package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.puerto.entrada.ConfigurarSistema;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioAuditoria;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConfiguracion;
import co.edu.uptc.asistente.dominio.excepcion.NoEncontradoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Configuracion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.RegistroAuditoria;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.util.List;
import java.util.Locale;

/** Parámetros del sistema (UC-16). Los cambios aplican de inmediato, sin reiniciar. */
public class ServicioConfiguracion implements ConfigurarSistema {
    private final RepositorioConfiguracion configuracion;
    private final RepositorioAuditoria auditoria;

    public ServicioConfiguracion(RepositorioConfiguracion configuracion, RepositorioAuditoria auditoria) {
        this.configuracion = configuracion;
        this.auditoria = auditoria;
    }

    @Override
    public Configuracion obtener() {
        return configuracion.obtener();
    }

    @Override
    public Configuracion actualizar(Configuracion nueva, Usuario actor) {
        Autorizacion.exigirSuperadministrador(actor);
        Configuracion anterior = configuracion.obtener();
        configuracion.guardar(nueva);
        auditoria.registrar(actor.getCorreo(), "Configurar sistema", String.format(Locale.ROOT,
                "umbral %.2f→%.2f, inactividad %d→%d min, correo %s",
                anterior.umbralConfianza(), nueva.umbralConfianza(), anterior.inactividadSesionMin(),
                nueva.inactividadSesionMin(), nueva.notificarPorCorreo() ? "sí" : "no"));
        return nueva;
    }

    @Override
    public Configuracion restaurar(Usuario actor) {
        Autorizacion.exigirSuperadministrador(actor);
        configuracion.guardar(Configuracion.POR_DEFECTO);
        auditoria.registrar(actor.getCorreo(), "Restaurar configuración", "Valores por defecto");
        return Configuracion.POR_DEFECTO;
    }

    @Override
    public Dependencia actualizarDependencia(String clave, Dependencia datos, Usuario actor) {
        Autorizacion.exigirSuperadministrador(actor);
        configuracion.dependencia(clave).orElseThrow(() -> new NoEncontradoException("La dependencia no existe"));
        String nombre = recortar(datos.nombre());
        if (nombre.length() < 3) {
            throw new ValidacionException("nombre", "Escribe el nombre oficial de la dependencia (RD-04)");
        }
        String correo = recortar(datos.correo());
        if (!correo.isEmpty() && !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ValidacionException("correo", "El correo no es válido");
        }
        String enlace = recortar(datos.enlace());
        if (!enlace.isEmpty() && !enlace.startsWith("https://") && !enlace.startsWith("http://")) {
            throw new ValidacionException("enlace", "El enlace debe empezar por https://");
        }
        Dependencia d = new Dependencia(clave, nombre, correo, recortar(datos.telefono()), enlace,
                recortar(datos.horario()));
        configuracion.guardarDependencia(d);
        auditoria.registrar(actor.getCorreo(), "Editar dependencia", clave);
        return d;
    }

    @Override
    public List<RegistroAuditoria> auditoria(int limite, Usuario actor) {
        Autorizacion.exigirSuperadministrador(actor);
        return auditoria.ultimos(Math.max(1, Math.min(limite, 500)));
    }

    private static String recortar(String s) {
        return s == null ? "" : s.strip();
    }
}
