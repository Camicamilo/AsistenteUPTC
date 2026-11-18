package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.SesionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SesionJpaRepository extends JpaRepository<SesionEntity, String> {
    List<SesionEntity> findByFechaFinIsNullAndUltimaActividadBefore(Instant limite);
}
