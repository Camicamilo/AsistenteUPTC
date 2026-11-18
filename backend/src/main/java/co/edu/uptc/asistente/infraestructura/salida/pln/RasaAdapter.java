package co.edu.uptc.asistente.infraestructura.salida.pln;

import co.edu.uptc.asistente.aplicacion.puerto.salida.ClasificadorIntencion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.EntrenadorModelo;
import co.edu.uptc.asistente.dominio.excepcion.PlnNoDisponibleException;
import co.edu.uptc.asistente.dominio.modelo.Entidad;
import co.edu.uptc.asistente.dominio.modelo.NivelConfianza;
import co.edu.uptc.asistente.dominio.modelo.ResultadoClasificacion;
import co.edu.uptc.asistente.infraestructura.configuracion.PropiedadesAsistente;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de salida hacia el motor PLN por HTTP/JSON (IF-02). Habla la API de Rasa 3
 * ({@code POST /model/parse}), así que sirve igual para un servidor Rasa real o para el servicio
 * PLN liviano del proyecto. Es la ÚNICA clase del backend que sabe que existe Rasa.
 */
@Component
public class RasaAdapter implements ClasificadorIntencion, EntrenadorModelo {
    private static final Logger log = LoggerFactory.getLogger(RasaAdapter.class);

    private final RestClient cliente;
    private final RestClient clienteEntrenamiento;
    private final String token;
    private final CircuitoPln circuito;

    public RasaAdapter(RestClient.Builder builder, PropiedadesAsistente propiedades, Clock reloj) {
        PropiedadesAsistente.Pln pln = propiedades.pln();
        this.cliente = builder.clone().baseUrl(pln.url()).requestFactory(fabrica(500, pln.timeoutMs())).build();
        this.clienteEntrenamiento = builder.clone().baseUrl(pln.url()).requestFactory(fabrica(1000, 30_000)).build();
        this.token = pln.token() == null ? "" : pln.token();
        this.circuito = new CircuitoPln(5, Duration.ofSeconds(30), reloj);
    }

    private static SimpleClientHttpRequestFactory fabrica(int conexionMs, int lecturaMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(conexionMs);
        f.setReadTimeout(lecturaMs);
        return f;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IntentRasa(String name, Double confidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EntidadRasa(String entity, Object value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaRasa(IntentRasa intent, List<EntidadRasa> entities) {
    }

    @Override
    public ResultadoClasificacion clasificar(String texto) {
        if (!circuito.permite()) {
            throw new PlnNoDisponibleException("Motor PLN no disponible (circuito abierto)", null);
        }
        try {
            RespuestaRasa r = cliente.post().uri("/model/parse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", texto))
                    .retrieve()
                    .body(RespuestaRasa.class);
            circuito.exito();
            return traducir(r);
        } catch (RestClientException e) {
            circuito.fallo();
            log.warn("El motor PLN no respondió: {}", e.getMessage());
            throw new PlnNoDisponibleException("Motor PLN no disponible", e);
        }
    }

    /** Traduce la respuesta de Rasa al modelo del dominio. */
    static ResultadoClasificacion traducir(RespuestaRasa r) {
        if (r == null || r.intent() == null || r.intent().name() == null) {
            return new ResultadoClasificacion(ResultadoClasificacion.FALLBACK, NivelConfianza.de(0), List.of());
        }
        List<Entidad> entidades = new ArrayList<>();
        if (r.entities() != null) {
            r.entities().forEach(e -> entidades.add(new Entidad(e.entity(), String.valueOf(e.value()))));
        }
        double confianza = r.intent().confidence() == null ? 0 : r.intent().confidence();
        return new ResultadoClasificacion(r.intent().name(), NivelConfianza.de(confianza), entidades);
    }

    @Override
    public void entrenar(Map<String, List<String>> ejemplosPorIntencion) {
        List<Map<String, Object>> nlu = new ArrayList<>();
        ejemplosPorIntencion.forEach((intent, ejemplos) -> nlu.add(Map.of("intent", intent, "examples", ejemplos)));
        try {
            clienteEntrenamiento.post()
                    .uri(u -> token.isBlank() ? u.path("/model/train").build()
                            : u.path("/model/train").queryParam("token", token).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("nlu", nlu))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new PlnNoDisponibleException("No se pudo reentrenar el motor PLN", e);
        }
        log.info("Motor PLN reentrenado con {} intenciones", nlu.size());
    }

    /** Usado por el monitor de disponibilidad. */
    public boolean saludable() {
        try {
            cliente.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    public String estadoCircuito() {
        return circuito.estado();
    }
}
