package co.edu.uptc.asistente.infraestructura.seguridad;

import co.edu.uptc.asistente.aplicacion.puerto.salida.CodificadorContrasenas;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Las contraseñas nunca se guardan en texto plano: BCrypt con costo 10. */
@Component
public class CodificadorBCrypt implements CodificadorContrasenas {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);

    @Override
    public String codificar(String contrasena) {
        return bcrypt.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        return hash != null && bcrypt.matches(contrasena, hash);
    }
}
