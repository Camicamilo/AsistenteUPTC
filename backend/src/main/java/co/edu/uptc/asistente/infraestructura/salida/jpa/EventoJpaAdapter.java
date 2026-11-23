package co.edu.uptc.asistente.infraestructura.salida.jpa;

import co.edu.uptc.asistente.aplicacion.puerto.salida.AlmacenEventos;
import co.edu.uptc.asistente.dominio.evento.EventoDominio;
import co.edu.uptc.asistente.dominio.modelo.EventoAlmacenado;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.EventoEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.EventoJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Almacén de eventos (EventSourcing 8.5): cada evento se agrega al final con la versión siguiente
 * de su agregado. Nunca se actualiza ni se borra una fila.
 */
@Component
public class EventoJpaAdapter implements AlmacenEventos {
    private static final int MAX_DATOS = 4000;

    private final EventoJpaRepository repo;
    private final ObjectMapper json;

    public EventoJpaAdapter(EventoJpaRepository repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @Override
    @Transactional
    public void agregar(EventoDominio evento) {
        int version = repo.ultimaVersion(evento.agregadoId()) + 1;
        repo.save(new EventoEntity(evento.agregadoId(), evento.tipoAgregado(), version, evento.tipoEvento(),
                serializar(evento), evento.fechaHora(), evento.usuarioId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoAlmacenado> delAgregado(String agregadoId) {
        return repo.findByAgregadoIdOrderByVersionAsc(agregadoId).stream()
                .map(e -> new EventoAlmacenado(e.getId(), e.getAgregadoId(), e.getTipoAgregado(), e.getVersion(),
                        e.getTipoEvento(), e.getDatos(), e.getFechaHora(), e.getUsuarioId()))
                .toList();
    }

    private String serializar(EventoDominio evento) {
        try {
            String s = json.writeValueAsString(evento);
            return s.length() > MAX_DATOS ? s.substring(0, MAX_DATOS) : s;
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
