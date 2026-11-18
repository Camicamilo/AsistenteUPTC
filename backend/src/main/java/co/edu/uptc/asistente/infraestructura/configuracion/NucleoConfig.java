package co.edu.uptc.asistente.infraestructura.configuracion;

import co.edu.uptc.asistente.aplicacion.casodeuso.ServicioAutenticacion;
import co.edu.uptc.asistente.aplicacion.casodeuso.ServicioConfiguracion;
import co.edu.uptc.asistente.aplicacion.casodeuso.ServicioConocimiento;
import co.edu.uptc.asistente.aplicacion.casodeuso.ServicioConversacion;
import co.edu.uptc.asistente.aplicacion.casodeuso.ServicioMetricas;
import co.edu.uptc.asistente.aplicacion.casodeuso.ServicioUsuarios;
import co.edu.uptc.asistente.aplicacion.puerto.salida.AlmacenEventos;
import co.edu.uptc.asistente.aplicacion.puerto.salida.ClasificadorIntencion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uptc.asistente.aplicacion.puerto.salida.EntrenadorModelo;
import co.edu.uptc.asistente.aplicacion.puerto.salida.MonitorDisponibilidad;
import co.edu.uptc.asistente.aplicacion.puerto.salida.NotificadorEscalamiento;
import co.edu.uptc.asistente.aplicacion.puerto.salida.PublicadorEventos;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioAuditoria;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConfiguracion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConsultas;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioIntenciones;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioSesiones;
import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uptc.asistente.dominio.servicio.SelectorRespuesta;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.random.RandomGenerator;

/**
 * Ensambla el núcleo del hexágono. Los casos de uso son clases Java simples (sin anotaciones de
 * Spring); aquí se les inyectan los adaptadores. Cambiar Rasa por Dialogflow o PostgreSQL por
 * MySQL solo cambia qué adaptador se registra, no el núcleo.
 */
@Configuration
public class NucleoConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public ZoneId zonaHoraria(PropiedadesAsistente propiedades) {
        return ZoneId.of(propiedades.zonaHoraria());
    }

    @Bean
    public ServicioConversacion servicioConversacion(ClasificadorIntencion clasificador,
                                                     RepositorioIntenciones intenciones,
                                                     RepositorioSesiones sesiones, RepositorioConsultas consultas,
                                                     RepositorioConfiguracion configuracion,
                                                     PublicadorEventos eventos, Clock reloj) {
        return new ServicioConversacion(clasificador, intenciones, sesiones, consultas, configuracion, eventos,
                new SelectorRespuesta(RandomGenerator.getDefault()), reloj);
    }

    @Bean
    public ServicioConocimiento servicioConocimiento(RepositorioIntenciones intenciones,
                                                     RepositorioConfiguracion configuracion,
                                                     PublicadorEventos eventos, AlmacenEventos almacen,
                                                     EntrenadorModelo entrenador, RepositorioAuditoria auditoria,
                                                     Clock reloj) {
        return new ServicioConocimiento(intenciones, configuracion, eventos, almacen, entrenador, auditoria, reloj);
    }

    @Bean
    public ServicioMetricas servicioMetricas(RepositorioConsultas consultas, RepositorioIntenciones intenciones,
                                             MonitorDisponibilidad monitor, Clock reloj, ZoneId zona) {
        return new ServicioMetricas(consultas, intenciones, monitor, reloj, zona);
    }

    @Bean
    public ServicioAutenticacion servicioAutenticacion(RepositorioUsuarios usuarios,
                                                       CodificadorContrasenas codificador, EmisorTokens tokens,
                                                       RepositorioAuditoria auditoria, Clock reloj) {
        return new ServicioAutenticacion(usuarios, codificador, tokens, auditoria, reloj);
    }

    @Bean
    public ServicioUsuarios servicioUsuarios(RepositorioUsuarios usuarios, CodificadorContrasenas codificador,
                                             NotificadorEscalamiento notificador, RepositorioAuditoria auditoria,
                                             Clock reloj) {
        return new ServicioUsuarios(usuarios, codificador, notificador, auditoria, reloj);
    }

    @Bean
    public ServicioConfiguracion servicioConfiguracion(RepositorioConfiguracion configuracion,
                                                       RepositorioAuditoria auditoria) {
        return new ServicioConfiguracion(configuracion, auditoria);
    }
}
