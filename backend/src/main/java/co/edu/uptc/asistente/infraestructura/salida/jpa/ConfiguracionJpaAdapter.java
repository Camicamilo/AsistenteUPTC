package co.edu.uptc.asistente.infraestructura.salida.jpa;

import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConfiguracion;
import co.edu.uptc.asistente.dominio.modelo.Configuracion;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.dominio.modelo.TemaRapido;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.ConfiguracionEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.entidad.DependenciaEntity;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.ConfiguracionJpaRepository;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.DependenciaJpaRepository;
import co.edu.uptc.asistente.infraestructura.salida.jpa.repositorio.TemaRapidoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Configuración, dependencias y temas rápidos. La configuración se lee en cada consulta, así que se
 * guarda en memoria y se refresca al modificarla (los cambios aplican sin reiniciar, UC-16).
 */
@Component
public class ConfiguracionJpaAdapter implements RepositorioConfiguracion {
    private static final int ID = 1;

    private final ConfiguracionJpaRepository configuraciones;
    private final DependenciaJpaRepository dependencias;
    private final TemaRapidoJpaRepository temas;
    private volatile Configuracion enMemoria;

    public ConfiguracionJpaAdapter(ConfiguracionJpaRepository configuraciones, DependenciaJpaRepository dependencias,
                                   TemaRapidoJpaRepository temas) {
        this.configuraciones = configuraciones;
        this.dependencias = dependencias;
        this.temas = temas;
    }

    @Override
    @Transactional(readOnly = true)
    public Configuracion obtener() {
        Configuracion c = enMemoria;
        if (c == null) {
            c = configuraciones.findById(ID)
                    .map(e -> new Configuracion(e.getUmbralConfianza().doubleValue(), e.getInactividadSesionMin(),
                            e.getPlantillaEscalamiento(), e.isNotificarPorCorreo()))
                    .orElse(Configuracion.POR_DEFECTO);
            enMemoria = c;
        }
        return c;
    }

    @Override
    @Transactional
    public void guardar(Configuracion c) {
        ConfiguracionEntity e = configuraciones.findById(ID).orElseGet(ConfiguracionEntity::new);
        e.setId(ID);
        e.setUmbralConfianza(BigDecimal.valueOf(c.umbralConfianza()).setScale(2, RoundingMode.HALF_UP));
        e.setInactividadSesionMin(c.inactividadSesionMin());
        e.setPlantillaEscalamiento(c.plantillaEscalamiento());
        e.setNotificarPorCorreo(c.notificarPorCorreo());
        configuraciones.save(e);
        enMemoria = c;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dependencia> dependencias() {
        return dependencias.findAll().stream().map(ConfiguracionJpaAdapter::aDominio)
                .sorted(Comparator.comparing(Dependencia::nombre)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Dependencia> dependencia(String clave) {
        return clave == null ? Optional.empty() : dependencias.findById(clave).map(ConfiguracionJpaAdapter::aDominio);
    }

    @Override
    @Transactional
    public void guardarDependencia(Dependencia d) {
        DependenciaEntity e = dependencias.findById(d.clave()).orElseGet(DependenciaEntity::new);
        e.setClave(d.clave());
        e.setNombre(d.nombre());
        e.setCorreo(d.correo());
        e.setTelefono(d.telefono());
        e.setEnlace(d.enlace());
        e.setHorario(d.horario());
        dependencias.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemaRapido> temasRapidos() {
        return temas.findAllByOrderByOrdenAsc().stream()
                .map(t -> new TemaRapido(t.getId(), t.getEtiqueta(), t.getConsulta())).toList();
    }

    private static Dependencia aDominio(DependenciaEntity e) {
        return new Dependencia(e.getClave(), e.getNombre(), vacio(e.getCorreo()), vacio(e.getTelefono()),
                vacio(e.getEnlace()), vacio(e.getHorario()));
    }

    private static String vacio(String s) {
        return s == null ? "" : s;
    }
}
