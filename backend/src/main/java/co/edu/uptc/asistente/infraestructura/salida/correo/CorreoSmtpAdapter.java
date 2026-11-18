package co.edu.uptc.asistente.infraestructura.salida.correo;

import co.edu.uptc.asistente.aplicacion.puerto.salida.NotificadorEscalamiento;
import co.edu.uptc.asistente.dominio.evento.ConsultaEscalada;
import co.edu.uptc.asistente.dominio.modelo.Dependencia;
import co.edu.uptc.asistente.infraestructura.configuracion.PropiedadesAsistente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Notificaciones por SMTP/TLS (IF-06). Por seguridad, si el correo no está habilitado solo se deja
 * en el log; y aunque esté habilitado, los escalamientos van al buzón configurado del piloto, no a
 * los correos reales de las dependencias, salvo que se active explícitamente.
 */
@Component
public class CorreoSmtpAdapter implements NotificadorEscalamiento {
    private static final Logger log = LoggerFactory.getLogger(CorreoSmtpAdapter.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender correo;
    private final PropiedadesAsistente.Correo config;
    private final ZoneId zona;

    public CorreoSmtpAdapter(JavaMailSender correo, PropiedadesAsistente propiedades, ZoneId zona) {
        this.correo = correo;
        this.config = propiedades.correo();
        this.zona = zona;
    }

    @Override
    public void notificar(ConsultaEscalada e, Dependencia dependencia) {
        String destino = config.usarCorreoDeDependencia() && !dependencia.correo().isBlank()
                ? dependencia.correo() : config.destinoEscalamiento();
        String cuerpo = """
                El Asistente Virtual no pudo resolver una consulta y la remitió a %s.

                Consulta: %s
                Motivo: %s
                Intención más probable: %s
                Fecha: %s
                Sesión (anónima): %s

                Este mensaje se generó automáticamente. No contiene datos personales del estudiante (Ley 1581 de 2012).
                """.formatted(dependencia.nombre(), e.texto(), e.motivo(),
                e.intent() == null ? "ninguna" : e.intent(), FECHA.format(e.fechaHora().atZone(zona)), e.sesionId());
        enviar(destino, "[Asistente UPTC] Consulta escalada a " + dependencia.nombre(), cuerpo);
    }

    @Override
    public void enviarCredenciales(String destinatario, String nombre, String contrasenaTemporal) {
        String cuerpo = """
                Hola %s,

                Se creó o restableció tu acceso al panel administrativo del Asistente Virtual UPTC.
                Usuario: %s
                Contraseña temporal: %s

                Cámbiala apenas ingreses (menú «Mi cuenta»).
                """.formatted(nombre, destinatario, contrasenaTemporal);
        enviar(destinatario, "[Asistente UPTC] Acceso al panel administrativo", cuerpo);
    }

    private void enviar(String destino, String asunto, String cuerpo) {
        if (!config.habilitado() || destino == null || destino.isBlank()) {
            log.info("Correo no enviado (deshabilitado o sin destino): {}", asunto);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(config.remitente());
            m.setTo(destino);
            m.setSubject(asunto);
            m.setText(cuerpo);
            correo.send(m);
        } catch (MailException ex) {
            // UC-11 E1: si falla el SMTP el estudiante igual recibe los datos de contacto.
            log.warn("No se pudo enviar el correo '{}': {}", asunto, ex.getMessage());
        }
    }
}
