package co.edu.uptc.asistente.infraestructura.salida.jpa;

import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioIntenciones;
import co.edu.uptc.asistente.dominio.excepcion.NoEncontradoException;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.Respuesta;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.EjemploEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.IntencionEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.RespuestaEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.IntencionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adaptador de salida RepositorioIntenciones -> JPA (RespuestaJpaAdapter del diseño 8.7). */
@Component
public class IntencionJpaAdapter implements RepositorioIntenciones {
    private final IntencionJpaRepository repo;

    public IntencionJpaAdapter(IntencionJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Intencion> listar() {
        return repo.findAll().stream().map(IntencionJpaAdapter::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Intencion> porId(long id) {
        return repo.findById(id).map(IntencionJpaAdapter::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Intencion> porNombre(String nombre) {
        return repo.findByNombre(nombre).map(IntencionJpaAdapter::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeNombre(String nombre) {
        return repo.existsByNombre(nombre);
    }

    @Override
    @Transactional
    public Intencion guardar(Intencion it) {
        IntencionEntity e;
        if (it.getId() == null) {
            e = new IntencionEntity();
            e.setNombre(it.getNombre());
            e.setSocial(it.isSocial());
        } else {
            e = repo.findById(it.getId()).orElseThrow(() -> new NoEncontradoException("La intención no existe"));
        }
        e.setCategoria(it.getCategoria());
        e.setDescripcion(it.getDescripcion());
        e.setDependencia(it.getDependencia());

        Map<Long, RespuestaEntity> existentes = new HashMap<>();
        e.getRespuestas().forEach(r -> existentes.put(r.getId(), r));
        for (Respuesta r : it.getRespuestas()) {
            RespuestaEntity re = r.getId() == null ? null : existentes.get(r.getId());
            if (re == null) {
                re = new RespuestaEntity();
                re.setIntencion(e);
                e.getRespuestas().add(re);
            }
            re.setTexto(r.getTexto());
            re.setActiva(r.isActiva());
            re.setFechaActualizacion(r.getFechaActualizacion());
            re.setActualizadoPor(r.getActualizadoPor());
        }

        List<String> actuales = e.getEjemplos().stream().map(EjemploEntity::getTexto).toList();
        if (!actuales.equals(it.getEjemplos())) {
            e.getEjemplos().clear();
            IntencionEntity duena = e;
            it.getEjemplos().forEach(t -> duena.getEjemplos().add(new EjemploEntity(duena, t)));
        }
        return aDominio(repo.saveAndFlush(e));
    }

    static Intencion aDominio(IntencionEntity e) {
        List<Respuesta> respuestas = e.getRespuestas().stream()
                .map(r -> new Respuesta(r.getId(), r.getTexto(), r.isActiva(), r.getFechaActualizacion(),
                        r.getActualizadoPor()))
                .toList();
        List<String> ejemplos = e.getEjemplos().stream().map(EjemploEntity::getTexto).toList();
        return new Intencion(e.getId(), e.getNombre(), e.getCategoria(), e.getDescripcion(), e.getDependencia(),
                e.isSocial(), ejemplos, respuestas);
    }
}
