package co.edu.uptc.asistente.infraestructura.entrada.programado;

import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarConocimiento;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.GestionarSesiones;
import co.edu.uptc.asistente.infraestructura.salida.jpa.DisponibilidadJpaAdapter;
import co.edu.uptc.asistente.infraestructura.salida.pln.RasaAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Adaptador de entrada "por tiempo": tareas que nadie pide desde HTTP.
 *  - cierre de sesiones inactivas (UC-09: 30 minutos por defecto)
 *  - chequeo de salud cada minuto para medir la disponibilidad real (RNF-02)
 *  - al arrancar, envía la base de conocimiento al motor PLN para que entrene con ella
 */
@Component
public class TareasProgramadas {
    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final GestionarSesiones sesiones;
    private final GestionarConocimiento conocimiento;
    private final RasaAdapter pln;
    private final DisponibilidadJpaAdapter disponibilidad;
    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public TareasProgramadas(GestionarSesiones sesiones, GestionarConocimiento conocimiento, RasaAdapter pln,
                             DisponibilidadJpaAdapter disponibilidad, JdbcTemplate jdbc, Clock reloj) {
        this.sesiones = sesiones;
        this.conocimiento = conocimiento;
        this.pln = pln;
        this.disponibilidad = disponibilidad;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void cerrarSesionesInactivas() {
        int cerradas = sesiones.cerrarInactivas();
        if (cerradas > 0) {
            log.info("Sesiones cerradas por inactividad: {}", cerradas);
        }
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT20S")
    public void chequearDisponibilidad() {
        boolean bdOk;
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            bdOk = true;
        } catch (RuntimeException e) {
            bdOk = false;
        }
        boolean plnOk = pln.saludable();
        if (bdOk) {
            disponibilidad.registrar(reloj.instant(), plnOk, true);
        }
        if (!plnOk || !bdOk) {
            log.warn("Chequeo de salud: PLN={} BD={}", plnOk, bdOk);
        }
    }

    /** El motor PLN puede arrancar después que el backend: se reintenta varias veces. */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarModeloAlIniciar() {
        for (int intento = 1; intento <= 12; intento++) {
            try {
                conocimiento.sincronizarModelo();
                log.info("Base de conocimiento sincronizada con el motor PLN");
                return;
            } catch (RuntimeException e) {
                log.info("Motor PLN aún no disponible (intento {}/12)", intento);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("No se pudo sincronizar la base de conocimiento con el motor PLN; usa POST /api/admin/pln/reentrenar");
    }
}
