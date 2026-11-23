package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.dominio.excepcion.NoAutorizadoException;
import co.edu.uptc.asistente.dominio.excepcion.ProhibidoException;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

/** Reglas de permisos del panel (UC-14, UC-16: solo el superadministrador). */
final class Autorizacion {
    private Autorizacion() {
    }

    static Usuario exigirSesion(Usuario actor) {
        if (actor == null || !actor.isActivo()) {
            throw new NoAutorizadoException("Sesión no válida");
        }
        return actor;
    }

    static Usuario exigirSuperadministrador(Usuario actor) {
        exigirSesion(actor);
        if (!actor.esSuperadministrador()) {
            throw new ProhibidoException("Esta acción solo la puede hacer un superadministrador");
        }
        return actor;
    }
}
