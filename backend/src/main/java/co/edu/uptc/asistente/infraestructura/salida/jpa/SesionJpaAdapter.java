package co.edu.uptc.asistente.infraestructura.salida.jpa;

import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioSesiones;
import co.edu.uptc.asistente.dominio.modelo.SesionChat;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.SesionEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.SesionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Adaptador de salida RepositorioSesiones -> JPA (SesionJpaAdapter del diseño 8.7). */
@Component
public class SesionJpaAdapter implements RepositorioSesiones {
    private final SesionJpaRepository repo;

    public SesionJpaAdapter(SesionJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void guardar(SesionChat s) {
        SesionEntity e = repo.findById(s.getSesionId()).orElseGet(SesionEntity::new);
        e.setId(s.getSesionId());
        e.setCanal(s.getCanal());
        e.setFechaInicio(s.getFechaInicio());
        e.setUltimaActividad(s.getUltimaActividad());
        e.setFechaFin(s.getFechaFin());
        e.setCausaCierre(s.getCausaCierre());
        e.setTotalConsultas(s.getTotalConsultas());
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SesionChat> porId(String sesionId) {
        return repo.findById(sesionId).map(SesionJpaAdapter::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionChat> abiertasSinActividadDesde(Instant limite) {
        return repo.findByFechaFinIsNullAndUltimaActividadBefore(limite).stream()
                .map(SesionJpaAdapter::aDominio).toList();
    }

    private static SesionChat aDominio(SesionEntity e) {
        return new SesionChat(e.getId(), e.getCanal(), e.getFechaInicio(), e.getUltimaActividad(), e.getFechaFin(),
                e.getCausaCierre(), e.getTotalConsultas());
    }
}
