package co.edu.uptc.asistente.aplicacion.puerto.salida;

public interface CodificadorContrasenas {
    String codificar(String contrasena);

    boolean coincide(String contrasena, String hash);
}
