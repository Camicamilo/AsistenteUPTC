package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.dto.UsuarioCreado;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarUsuarios;
import co.edu.uptc.asistente.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.NotificadorEscalamiento;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioAuditoria;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uptc.asistente.dominio.excepcion.ConflictoException;
import co.edu.uptc.asistente.dominio.excepcion.NoEncontradoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.dominio.modelo.Rol;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;

/** Gestión de cuentas del panel (UC-14). Desactivar, nunca borrar: se conserva el historial. */
public class ServicioUsuarios implements GestionarUsuarios {
    private final RepositorioUsuarios usuarios;
    private final CodificadorContrasenas codificador;
    private final NotificadorEscalamiento notificador;
    private final RepositorioAuditoria auditoria;
    private final Clock reloj;

    public ServicioUsuarios(RepositorioUsuarios usuarios, CodificadorContrasenas codificador,
                            NotificadorEscalamiento notificador, RepositorioAuditoria auditoria, Clock reloj) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Override
    public List<Usuario> listar(Usuario actor) {
        Autorizacion.exigirSuperadministrador(actor);
        return usuarios.listar().stream().sorted(Comparator.comparing(Usuario::getNombre)).toList();
    }

    @Override
    public UsuarioCreado crear(String nombre, String correo, String rol, Usuario actor) {
        Autorizacion.exigirSuperadministrador(actor);
        String c = Usuario.normalizarCorreo(correo);
        if (usuarios.porCorreo(c).isPresent()) {
            throw new ConflictoException("correo", "Ya existe un usuario con ese correo");
        }
        String temporal = GeneradorContrasenas.generar();
        Usuario nuevo = usuarios.guardar(new Usuario(null, nombre, c, codificador.codificar(temporal), Rol.de(rol),
                true, true, reloj.instant(), null));
        notificador.enviarCredenciales(nuevo.getCorreo(), nuevo.getNombre(), temporal);
        auditoria.registrar(actor.getCorreo(), "Crear usuario", nuevo.getCorreo() + " (" + nuevo.getRol().valor() + ")");
        return new UsuarioCreado(nuevo, temporal);
    }

    @Override
    public Usuario actualizar(long id, String nombre, String rol, Boolean activo, Usuario actor) {
        Autorizacion.exigirSuperadministrador(actor);
        Usuario u = buscar(id);
        Rol nuevoRol = rol == null || rol.isBlank() ? null : Rol.de(rol);
        if (u.getId().equals(actor.getId())) {
            if (Boolean.FALSE.equals(activo)) {
                throw new ValidacionException("activo", "No puedes desactivar tu propia cuenta");
            }
            if (nuevoRol != null && nuevoRol != Rol.SUPERADMINISTRADOR) {
                throw new ValidacionException("rol", "No puedes quitarte el rol de superadministrador");
            }
        }
        u.actualizar(nombre, nuevoRol, activo);
        Usuario guardado = usuarios.guardar(u);
        String accion = Boolean.FALSE.equals(activo) ? "Desactivar usuario"
                : Boolean.TRUE.equals(activo) ? "Activar usuario" : "Editar usuario";
        auditoria.registrar(actor.getCorreo(), accion, u.getCorreo());
        return guardado;
    }

    @Override
    public String restablecerContrasena(long id, Usuario actor) {
        Autorizacion.exigirSuperadministrador(actor);
        Usuario u = buscar(id);
        String temporal = GeneradorContrasenas.generar();
        u.cambiarContrasena(codificador.codificar(temporal), true);
        usuarios.guardar(u);
        notificador.enviarCredenciales(u.getCorreo(), u.getNombre(), temporal);
        auditoria.registrar(actor.getCorreo(), "Restablecer contraseña", u.getCorreo());
        return temporal;
    }

    private Usuario buscar(long id) {
        return usuarios.porId(id).orElseThrow(() -> new NoEncontradoException("El usuario no existe"));
    }
}
