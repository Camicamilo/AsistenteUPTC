package co.edu.uptc.asistente.infraestructura.salida.jpa;

import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioAuditoria;
import co.edu.uptc.asistente.dominio.modelo.RegistroAuditoria;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.AuditoriaEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.AuditoriaJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Component
public class AuditoriaJpaAdapter implements RepositorioAuditoria {
    private final AuditoriaJpaRepository repo;
    private final Clock reloj;

    public AuditoriaJpaAdapter(AuditoriaJpaRepository repo, Clock reloj) {
        this.repo = repo;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void registrar(String usuario, String accion, String detalle) {
        String d = detalle == null ? null : detalle.length() > 500 ? detalle.substring(0, 500) : detalle;
        repo.save(new AuditoriaEntity(reloj.instant(), usuario, accion, d));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAuditoria> ultimos(int limite) {
        return repo.findAllByOrderByFechaDescIdDesc(PageRequest.of(0, limite)).stream()
                .map(e -> new RegistroAuditoria(e.getId(), e.getFecha(), e.getUsuario(), e.getAccion(), e.getDetalle()))
                .toList();
    }
}
