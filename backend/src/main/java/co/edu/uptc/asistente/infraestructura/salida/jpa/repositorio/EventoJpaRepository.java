package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.EventoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventoJpaRepository extends JpaRepository<EventoEntity, Long> {
    List<EventoEntity> findByAgregadoIdOrderByVersionAsc(String agregadoId);

    @Query("select coalesce(max(e.version), 0) from EventoEntity e where e.agregadoId = :agregadoId")
    int ultimaVersion(@Param("agregadoId") String agregadoId);
}
