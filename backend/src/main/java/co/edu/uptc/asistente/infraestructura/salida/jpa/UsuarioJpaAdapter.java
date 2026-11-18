package co.edu.uptc.asistente.infraestructura.salida.jpa;

import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uptc.asistente.dominio.modelo.Rol;
import co.edu.uptc.asistente.dominio.modelo.Usuario;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.UsuarioEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.UsuarioJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class UsuarioJpaAdapter implements RepositorioUsuarios {
    private final UsuarioJpaRepository repo;

    public UsuarioJpaAdapter(UsuarioJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> porId(long id) {
        return repo.findById(id).map(UsuarioJpaAdapter::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> porCorreo(String correo) {
        return repo.findByCorreo(correo).map(UsuarioJpaAdapter::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repo.findAll().stream().map(UsuarioJpaAdapter::aDominio).toList();
    }

    @Override
    @Transactional
    public Usuario guardar(Usuario u) {
        UsuarioEntity e = u.getId() == null ? new UsuarioEntity() : repo.findById(u.getId()).orElseGet(UsuarioEntity::new);
        e.setNombre(u.getNombre());
        e.setCorreo(u.getCorreo());
        e.setPasswordHash(u.getPasswordHash());
        e.setRol(u.getRol().valor());
        e.setActivo(u.isActivo());
        e.setDebeCambiarPassword(u.isDebeCambiarPassword());
        e.setFechaRegistro(u.getFechaRegistro());
        e.setUltimoAcceso(u.getUltimoAcceso());
        UsuarioEntity g = repo.save(e);
        u.asignarId(g.getId());
        return aDominio(g);
    }

    private static Usuario aDominio(UsuarioEntity e) {
        return new Usuario(e.getId(), e.getNombre(), e.getCorreo(), e.getPasswordHash(), Rol.de(e.getRol()),
                e.isActivo(), e.isDebeCambiarPassword(), e.getFechaRegistro(), e.getUltimoAcceso());
    }
}
