package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.IntencionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntencionJpaRepository extends JpaRepository<IntencionEntity, Long> {
    Optional<IntencionEntity> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
