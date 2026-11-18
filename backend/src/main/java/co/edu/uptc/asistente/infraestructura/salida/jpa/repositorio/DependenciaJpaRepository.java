package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.DependenciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenciaJpaRepository extends JpaRepository<DependenciaEntity, String> {
}
