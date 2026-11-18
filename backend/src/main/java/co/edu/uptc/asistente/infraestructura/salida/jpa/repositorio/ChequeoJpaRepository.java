package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.ChequeoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ChequeoJpaRepository extends JpaRepository<ChequeoEntity, Long> {
    long countByFechaHoraBetween(Instant desde, Instant hasta);

    long countByFechaHoraBetweenAndPlnOkTrueAndBdOkTrue(Instant desde, Instant hasta);
}
