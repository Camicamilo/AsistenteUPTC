package co.edu.uptc.asistente.dominio.modelo;

import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/** Usuario del panel administrativo. Solo correos @uptc.edu.co (UC-14). */
public class Usuario {
    public static final String DOMINIO = "@uptc.edu.co";
    private static final Pattern CORREO = Pattern.compile("^[a-z0-9._%+-]+@uptc\\.edu\\.co$");

    private Long id;
    private String nombre;
    private final String correo;
    private String passwordHash;
    private Rol rol;
    private boolean activo;
    private boolean debeCambiarPassword;
    private final Instant fechaRegistro;
    private Instant ultimoAcceso;

    public Usuario(Long id, String nombre, String correo, String passwordHash, Rol rol, boolean activo,
                   boolean debeCambiarPassword, Instant fechaRegistro, Instant ultimoAcceso) {
        this.id = id;
        this.nombre = validarNombre(nombre);
        this.correo = normalizarCorreo(correo);
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
        this.debeCambiarPassword = debeCambiarPassword;
        this.fechaRegistro = fechaRegistro;
        this.ultimoAcceso = ultimoAcceso;
    }

    public static String normalizarCorreo(String correo) {
        String c = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        if (!CORREO.matcher(c).matches()) {
            throw new ValidacionException("correo", "Solo se permiten correos institucionales " + DOMINIO);
        }
        return c;
    }

    private static String validarNombre(String nombre) {
        String n = nombre == null ? "" : nombre.strip();
        if (n.length() < 3 || n.length() > 120) {
            throw new ValidacionException("nombre", "El nombre debe tener entre 3 y 120 caracteres");
        }
        return n;
    }

    /** Política mínima de contraseñas del panel. */
    public static void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < 8 || !contrasena.matches(".*[A-Za-z].*")
                || !contrasena.matches(".*\\d.*")) {
            throw new ValidacionException("nueva", "La contraseña debe tener mínimo 8 caracteres, letras y números");
        }
    }

    public boolean esSuperadministrador() {
        return rol == Rol.SUPERADMINISTRADOR;
    }

    public void registrarAcceso(Instant ahora) {
        this.ultimoAcceso = ahora;
    }

    public void cambiarContrasena(String nuevoHash, boolean temporal) {
        this.passwordHash = nuevoHash;
        this.debeCambiarPassword = temporal;
    }

    public void actualizar(String nombre, Rol rol, Boolean activo) {
        if (nombre != null) {
            this.nombre = validarNombre(nombre);
        }
        if (rol != null) {
            this.rol = rol;
        }
        if (activo != null) {
            this.activo = activo;
        }
    }

    public Long getId() { return id; }
    public void asignarId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public Instant getUltimoAcceso() { return ultimoAcceso; }
}
