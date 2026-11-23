package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByCorreo(String correo);
}
