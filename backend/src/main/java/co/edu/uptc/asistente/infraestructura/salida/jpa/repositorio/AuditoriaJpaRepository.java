package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.AuditoriaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaJpaRepository extends JpaRepository<AuditoriaEntity, Long> {
    List<AuditoriaEntity> findAllByOrderByFechaDescIdDesc(Pageable pagina);
}
