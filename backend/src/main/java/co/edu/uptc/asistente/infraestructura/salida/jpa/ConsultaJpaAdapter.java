package co.edu.uptc.asistente.infraestructura.salida.jpa;

import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConsultas;
import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.ConsultaEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.ConsultaJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/** Registro de interacciones (RF-06). Nunca guarda datos personales: solo el texto y la sesión anónima. */
@Component
public class ConsultaJpaAdapter implements RepositorioConsultas {
    private final ConsultaJpaRepository repo;

    public ConsultaJpaAdapter(ConsultaJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void registrar(RegistroConsulta r) {
        ConsultaEntity e = new ConsultaEntity();
        e.setSesionId(r.sesionId());
        e.setTexto(r.texto());
        e.setIntent(r.intent());
        e.setCategoria(r.categoria());
        e.setConfianza(r.confianza() == null ? null
                : BigDecimal.valueOf(r.confianza()).setScale(3, RoundingMode.HALF_UP));
        e.setResuelta(r.resuelta());
        e.setMotivoEscalamiento(r.motivoEscalamiento());
        e.setRespuestaId(r.respuestaId());
        e.setTiempoMs((int) Math.min(Integer.MAX_VALUE, r.tiempoMs()));
        e.setCanal(r.canal());
        e.setFechaHora(r.fechaHora());
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroConsulta> entre(Instant desde, Instant hasta) {
        return repo.findByFechaHoraBetweenOrderByFechaHoraDesc(desde, hasta).stream()
                .map(e -> new RegistroConsulta(e.getId(), e.getSesionId(), e.getTexto(), e.getIntent(),
                        e.getCategoria(), e.getConfianza() == null ? null : e.getConfianza().doubleValue(),
                        e.isResuelta(), e.getMotivoEscalamiento(), e.getRespuestaId(), e.getTiempoMs(), e.getCanal(),
                        e.getFechaHora()))
                .toList();
    }
}
