package co.edu.uptc.asistente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Asistente Virtual Automatizado UPTC.
 *
 * <p>Arquitectura hexagonal (pauta 8.7): {@code dominio} y {@code aplicacion} forman el núcleo y no
 * dependen de Spring; {@code infraestructura} contiene los adaptadores de entrada (REST, tareas
 * programadas) y de salida (PLN, JPA, correo, eventos).</p>
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class AsistenteApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsistenteApplication.class, args);
    }
}
