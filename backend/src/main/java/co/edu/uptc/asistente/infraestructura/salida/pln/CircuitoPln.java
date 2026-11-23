package co.edu.uptc.asistente.infraestructura.salida.pln;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Circuit Breaker sencillo (pauta 8.6.3): después de N fallos seguidos el circuito se "abre" y
 * durante un tiempo se responde de inmediato con el FallbackHandler, sin esperar el timeout del
 * motor PLN. Pasado ese tiempo deja pasar una petición de prueba (semiabierto).
 */
public class CircuitoPln {
    private final int fallosParaAbrir;
    private final Duration tiempoAbierto;
    private final Clock reloj;
    private int fallosSeguidos;
    private Instant abiertoHasta;

    public CircuitoPln(int fallosParaAbrir, Duration tiempoAbierto, Clock reloj) {
        this.fallosParaAbrir = fallosParaAbrir;
        this.tiempoAbierto = tiempoAbierto;
        this.reloj = reloj;
    }

    public synchronized boolean permite() {
        return abiertoHasta == null || !reloj.instant().isBefore(abiertoHasta);
    }

    public synchronized void exito() {
        fallosSeguidos = 0;
        abiertoHasta = null;
    }

    public synchronized void fallo() {
        fallosSeguidos++;
        if (fallosSeguidos >= fallosParaAbrir) {
            abiertoHasta = reloj.instant().plus(tiempoAbierto);
        }
    }

    public synchronized String estado() {
        if (abiertoHasta == null) {
            return "cerrado";
        }
        return reloj.instant().isBefore(abiertoHasta) ? "abierto" : "semiabierto";
    }
}
