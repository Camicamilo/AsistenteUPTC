package co.edu.uptc.asistente.infraestructura.salida.eventos;

import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarConocimiento;
import co.edu.uptc.asistente.aplicacion.puerto.salida.AlmacenEventos;
import co.edu.uptc.asistente.aplicacion.puerto.salida.NotificadorEscalamiento;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConfiguracion;
import co.edu.uptc.asistente.dominio.evento.ConocimientoActualizado;
import co.edu.uptc.asistente.dominio.evento.ConsultaEscalada;
import co.edu.uptc.asistente.dominio.evento.EventoDominio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Consumidores de los eventos del dominio. Lo que el estudiante no tiene por qué esperar
 * (correo, reentrenamiento) se hace en segundo plano con @Async.
 */
@Component
public class ManejadorEventos {
    private static final Logger log = LoggerFactory.getLogger(ManejadorEventos.class);

    private final AlmacenEventos almacen;
    private final NotificadorEscalamiento notificador;
    private final RepositorioConfiguracion configuracion;
    private final GestionarConocimiento conocimiento;

    public ManejadorEventos(AlmacenEventos almacen, NotificadorEscalamiento notificador,
                            RepositorioConfiguracion configuracion, GestionarConocimiento conocimiento) {
        this.almacen = almacen;
        this.notificador = notificador;
        this.configuracion = configuracion;
        this.conocimiento = conocimiento;
    }

    /** Todo evento queda en el almacén de eventos (EventSourcing). Un fallo aquí no tumba el chat. */
    @EventListener
    public void guardar(EventoDominio evento) {
        try {
            almacen.agregar(evento);
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el evento {} de {}: {}", evento.tipoEvento(), evento.agregadoId(),
                    e.getMessage());
        }
    }

    @Async
    @EventListener
    public void notificarEscalamiento(ConsultaEscalada evento) {
        if (!configuracion.obtener().notificarPorCorreo()) {
            return;
        }
        configuracion.dependencia(evento.dependencia())
                .ifPresent(d -> notificador.notificar(evento, d));
    }

    @Async
    @EventListener
    public void reentrenar(ConocimientoActualizado evento) {
        if (!evento.requiereReentrenar()) {
            return;
        }
        try {
            conocimiento.sincronizarModelo();
        } catch (RuntimeException e) {
            log.warn("No se pudo reentrenar el motor PLN tras crear '{}': {}", evento.intent(), e.getMessage());
        }
    }
}
