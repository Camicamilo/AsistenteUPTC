package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.ConsultaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ConsultaJpaRepository extends JpaRepository<ConsultaEntity, Long> {
    List<ConsultaEntity> findByFechaHoraBetweenOrderByFechaHoraDesc(Instant desde, Instant hasta);
}
