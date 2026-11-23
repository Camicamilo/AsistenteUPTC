package co.edu.uptc.asistente.aplicacion.puerto.entrada;

import co.edu.uptc.asistente.aplicacion.dto.NuevaIntencion;
import co.edu.uptc.asistente.aplicacion.dto.Pagina;
import co.edu.uptc.asistente.aplicacion.dto.ResumenIntencion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.EventoAlmacenado;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.Usuario;

import java.util.List;

/** Base de conocimiento desde el panel (UC-12, UC-13). */
public interface GestionarConocimiento {
    Pagina<ResumenIntencion> listar(String q, String categoria, int page, int pageSize);

    Intencion obtener(long id);

    Intencion crear(NuevaIntencion datos, Usuario actor);

    Intencion actualizar(long id, String categoria, String descripcion, String dependencia, Usuario actor);

    Intencion agregarRespuesta(long id, String texto, Usuario actor);

    Intencion actualizarRespuesta(long id, long respuestaId, String texto, Boolean activa, Usuario actor);

    /** Historial de cambios de la intención leído del almacén de eventos (EventSourcing). */
    List<EventoAlmacenado> historial(long id);

    List<Dependencia> dependencias();

    /** Envía todas las intenciones y ejemplos al motor PLN para que reentrene. */
    void sincronizarModelo();
}
