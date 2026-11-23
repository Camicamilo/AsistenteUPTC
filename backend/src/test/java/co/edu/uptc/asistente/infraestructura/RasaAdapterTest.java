package co.edu.uptc.asistente.infraestructura;

import co.edu.uptc.asistente.dominio.excepcion.PlnNoDisponibleException;
import co.edu.uptc.asistente.dominio.modelo.ResultadoClasificacion;
import co.edu.uptc.asistente.infraestructura.configuracion.PropiedadesAsistente;
import co.edu.uptc.asistente.infraestructura.salida.pln.RasaAdapter;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba del adaptador contra un servidor HTTP que imita la API de Rasa. */
class RasaAdapterTest {
    private HttpServer servidor;
    private volatile String respuesta;
    private volatile int demoraMs;

    @BeforeEach
    void iniciar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/model/parse", ex -> {
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] b = respuesta.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        });
        servidor.createContext("/health", ex -> {
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private RasaAdapter adaptador(String url) {
        PropiedadesAsistente p = new PropiedadesAsistente("America/Bogota",
                new PropiedadesAsistente.Pln(url, "", 300),
                new PropiedadesAsistente.Jwt("secreto", 8),
                new PropiedadesAsistente.Cors(List.of()),
                new PropiedadesAsistente.Correo(false, "x@uptc.edu.co", "", false));
        return new RasaAdapter(RestClient.builder(), p, Clock.systemUTC());
    }

    private String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @Test
    void traduceLaRespuestaDeRasaAlDominio() {
        respuesta = """
                {"text":"x","intent":{"name":"consulta_matricula","confidence":0.87},
                 "entities":[{"entity":"periodo","value":"2027-I","extractor":"RegexEntityExtractor"}],
                 "intent_ranking":[]}""";
        ResultadoClasificacion r = adaptador(url()).clasificar("¿Cuándo son las matrículas 2027-1?");
        assertEquals("consulta_matricula", r.intencion());
        assertEquals(0.87, r.confianza().valor(), 1e-9);
        assertEquals("periodo", r.entidades().get(0).tipo());
        assertEquals("2027-I", r.entidades().get(0).valor());
    }

    @Test
    void sinIntencionSeTrataComoFueraDeDominio() {
        respuesta = "{\"text\":\"x\",\"intent\":{\"name\":null,\"confidence\":0.0},\"entities\":[]}";
        assertTrue(adaptador(url()).clasificar("x").esFueraDeDominio());
    }

    @Test
    void timeoutLanzaPlnNoDisponible() {
        respuesta = "{}";
        demoraMs = 1000; // el adaptador espera máximo 300 ms en esta prueba
        assertThrows(PlnNoDisponibleException.class, () -> adaptador(url()).clasificar("hola"));
    }

    @Test
    void elCircuitoSeAbreDespuesDeCincoFallos() {
        RasaAdapter a = adaptador("http://127.0.0.1:1"); // puerto sin servicio
        for (int i = 0; i < 5; i++) {
            assertThrows(PlnNoDisponibleException.class, () -> a.clasificar("hola"));
        }
        assertEquals("abierto", a.estadoCircuito());
        assertFalse(a.saludable());
    }

    @Test
    void saludDelMotor() {
        assertTrue(adaptador(url()).saludable());
    }
}
