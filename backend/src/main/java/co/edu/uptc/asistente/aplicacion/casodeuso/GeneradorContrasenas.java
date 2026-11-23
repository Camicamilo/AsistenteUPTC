package co.edu.uptc.asistente.aplicacion.casodeuso;

import java.security.SecureRandom;

/** Contraseñas temporales seguras (cumplen la política de Usuario.validarContrasena). */
final class GeneradorContrasenas {
    private static final String LETRAS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITOS = "23456789";
    private static final String SIMBOLOS = "*#$%&";
    private static final SecureRandom AZAR = new SecureRandom();

    private GeneradorContrasenas() {
    }

    static String generar() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(LETRAS.charAt(AZAR.nextInt(LETRAS.length())));
        }
        sb.append(DIGITOS.charAt(AZAR.nextInt(DIGITOS.length())));
        sb.append(DIGITOS.charAt(AZAR.nextInt(DIGITOS.length())));
        sb.append(SIMBOLOS.charAt(AZAR.nextInt(SIMBOLOS.length())));
        sb.append(LETRAS.charAt(AZAR.nextInt(LETRAS.length())));
        return sb.toString();
    }
}
