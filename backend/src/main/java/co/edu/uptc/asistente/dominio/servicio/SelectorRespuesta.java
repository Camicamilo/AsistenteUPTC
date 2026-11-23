package co.edu.uptc.asistente.dominio.servicio;

import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.Respuesta;

import java.util.List;
import java.util.random.RandomGenerator;

/** Elige una de las respuestas activas para que el asistente no suene repetitivo. */
public class SelectorRespuesta {
    private final RandomGenerator azar;

    public SelectorRespuesta(RandomGenerator azar) {
        this.azar = azar;
    }

    public Respuesta elegir(Intencion intencion) {
        List<Respuesta> activas = intencion.respuestasActivas();
        if (activas.isEmpty()) {
            throw new IllegalStateException("La intención no tiene respuestas activas");
        }
        return activas.get(azar.nextInt(activas.size()));
    }
}
