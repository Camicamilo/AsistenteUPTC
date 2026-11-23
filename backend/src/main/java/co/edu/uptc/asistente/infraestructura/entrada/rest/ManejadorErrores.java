package co.edu.uptc.asistente.infraestructura.entrada.rest;

import co.edu.uptc.asistente.aplicacion.puerto.salida.RepositorioConfiguracion;
import co.edu.uptc.asistente.dominio.excepcion.ConflictoException;
import co.edu.uptc.asistente.dominio.excepcion.NoAutorizadoException;
import co.edu.uptc.asistente.dominio.excepcion.NoEncontradoException;
import co.edu.uptc.asistente.dominio.excepcion.PlnNoDisponibleException;
import co.edu.uptc.asistente.dominio.excepcion.ProhibidoException;
import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;
import co.edu.uptc.asistente.infraestructura.entrada.rest.dto.Respuestas.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce las excepciones del dominio a códigos HTTP. Ningún error deja al chat colgado (RNF-08):
 * siempre hay un cuerpo JSON con un mensaje que el frontend muestra.
 */
@RestControllerAdvice
public class ManejadorErrores {
    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private final RepositorioConfiguracion configuracion;

    public ManejadorErrores(RepositorioConfiguracion configuracion) {
        this.configuracion = configuracion;
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorDto> validacion(ValidacionException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), e.getCampo(), "VALIDACION");
    }

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorDto> noEncontrado(NoEncontradoException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), null, "NO_ENCONTRADO");
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorDto> conflicto(ConflictoException e) {
        String codigo = "sesionId".equals(e.getCampo()) ? "SESION_CERRADA" : "CONFLICTO";
        return error(HttpStatus.CONFLICT, e.getMessage(), e.getCampo(), codigo);
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<ErrorDto> noAutorizado(NoAutorizadoException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage(), null, "NO_AUTORIZADO");
    }

    @ExceptionHandler(ProhibidoException.class)
    public ResponseEntity<ErrorDto> prohibido(ProhibidoException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage(), null, "PROHIBIDO");
    }

    /** UC-05 E1: el motor PLN no está; se informa y se ofrecen los datos de atención al ciudadano. */
    @ExceptionHandler(PlnNoDisponibleException.class)
    public ResponseEntity<ErrorDto> plnNoDisponible(PlnNoDisponibleException e) {
        Object dependencia = null;
        try {
            dependencia = configuracion.dependencia("general").orElse(null);
        } catch (RuntimeException ignorada) {
            // si tampoco hay base de datos, se responde sin datos de contacto
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorDto("El servicio del asistente no está disponible temporalmente", null,
                        "PLN_NO_DISPONIBLE", dependencia));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorDto> peticionInvalida(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "La petición no tiene el formato esperado", null, "VALIDACION");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDto> rutaInexistente(NoResourceFoundException e) {
        return error(HttpStatus.NOT_FOUND, "Ruta no encontrada", null, "NO_ENCONTRADO");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDto> metodo(HttpRequestMethodNotSupportedException e) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido", null, "METODO");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> inesperado(Exception e) {
        log.error("Error inesperado", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", null, "SERVIDOR");
    }

    private static ResponseEntity<ErrorDto> error(HttpStatus estado, String mensaje, String campo, String codigo) {
        return ResponseEntity.status(estado).body(new ErrorDto(mensaje, campo, codigo, null));
    }
}
