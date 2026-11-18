package co.edu.uptc.asistente.infraestructura.salida.eventos;

import co.edu.uptc.asistente.aplicacion.puerto.salida.PublicadorEventos;
import co.edu.uptc.asistente.dominio.evento.EventoDominio;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Bus interno de eventos: eventos de aplicación de Spring (patrón Observer, 8.4.4). */
@Component
public class PublicadorEventosSpring implements PublicadorEventos {
    private final ApplicationEventPublisher publicador;

    public PublicadorEventosSpring(ApplicationEventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(EventoDominio evento) {
        publicador.publishEvent(evento);
    }
}
