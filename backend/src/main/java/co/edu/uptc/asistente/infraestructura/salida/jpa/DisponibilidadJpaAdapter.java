package co.edu.uptc.asistente.infraestructura.salida.jpa;

import co.edu.uptc.asistente.aplicacion.puerto.salida.MonitorDisponibilidad;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.ChequeoEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.ChequeoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.OptionalDouble;

/** Disponibilidad real = chequeos exitosos / chequeos totales del periodo (RNF-02: mínimo 95 %). */
@Component
public class DisponibilidadJpaAdapter implements MonitorDisponibilidad {
    private final ChequeoJpaRepository repo;

    public DisponibilidadJpaAdapter(ChequeoJpaRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void registrar(Instant fecha, boolean plnOk, boolean bdOk) {
        repo.save(new ChequeoEntity(fecha, plnOk, bdOk));
    }

    @Override
    @Transactional(readOnly = true)
    public OptionalDouble disponibilidad(Instant desde, Instant hasta) {
        long total = repo.countByFechaHoraBetween(desde, hasta);
        if (total == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(repo.countByFechaHoraBetweenAndPlnOkTrueAndBdOkTrue(desde, hasta) / (double) total);
    }
}
