package co.edu.uptc.asistente.infraestructura.salida.cache;

import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioIntenciones;
import co.edu.uptc.asistente.dominio.evento.ConocimientoActualizado;
import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.infraestructura.salida.jpa.IntencionJpaAdapter;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lado de LECTURA de CQRS (pauta 8.4.6): caché en memoria de la base de conocimiento para que el
 * chat no consulte la base de datos en cada mensaje. Se invalida con el evento ConocimientoActualizado
 * y, por si hay varias instancias, también cada 60 s (RF-07 pide que un cambio se vea en menos de 5 min).
 */
@Primary
@Component
public class IntencionesConCache implements RepositorioIntenciones {
    private static final Duration VIGENCIA = Duration.ofSeconds(60);

    private final IntencionJpaAdapter origen;
    private final Clock reloj;
    private volatile Instantanea instantanea;

    private record Instantanea(List<Intencion> lista, Map<String, Intencion> porNombre, Instant cargada) {
    }

    public IntencionesConCache(IntencionJpaAdapter origen, Clock reloj) {
        this.origen = origen;
        this.reloj = reloj;
    }

    private Instantanea vigente() {
        Instantanea i = instantanea;
        if (i == null || reloj.instant().isAfter(i.cargada().plus(VIGENCIA))) {
            List<Intencion> lista = origen.listar();
            i = new Instantanea(lista, lista.stream().collect(Collectors.toMap(Intencion::getNombre,
                    Function.identity(), (a, b) -> a)), reloj.instant());
            instantanea = i;
        }
        return i;
    }

    /** Copia de solo lectura para el chat. */
    @Override
    public List<Intencion> listar() {
        return vigente().lista();
    }

    @Override
    public Optional<Intencion> porNombre(String nombre) {
        return Optional.ofNullable(vigente().porNombre().get(nombre));
    }

    /** Las escrituras y lecturas para editar van directo a la base de datos (lado de COMANDOS). */
    @Override
    public Optional<Intencion> porId(long id) {
        return origen.porId(id);
    }

    @Override
    public boolean existeNombre(String nombre) {
        return origen.existeNombre(nombre);
    }

    @Override
    public Intencion guardar(Intencion intencion) {
        Intencion guardada = origen.guardar(intencion);
        invalidar();
        return guardada;
    }

    @EventListener
    public void alCambiarConocimiento(ConocimientoActualizado evento) {
        invalidar();
    }

    public void invalidar() {
        instantanea = null;
    }
}
