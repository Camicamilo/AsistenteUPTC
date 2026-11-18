package co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio;

import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.TemaRapidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemaRapidoJpaRepository extends JpaRepository<TemaRapidoEntity, String> {
    List<TemaRapidoEntity> findAllByOrderByOrdenAsc();
}
