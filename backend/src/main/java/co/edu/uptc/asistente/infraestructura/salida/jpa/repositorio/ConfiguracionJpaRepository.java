package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.ConfiguracionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionJpaRepository extends JpaRepository<ConfiguracionEntity, Integer> {
}
