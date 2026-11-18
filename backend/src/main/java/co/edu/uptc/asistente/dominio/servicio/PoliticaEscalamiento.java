package co.edu.uptc.asistente.dominio.servicio;

import co.edu.uptc.asistente.dominio.modelo.Intencion;
import co.edu.uptc.asistente.dominio.modelo.MotivoEscalamiento;
import co.edu.uptc.asistente.dominio.modelo.ResultadoClasificacion;

import java.util.Optional;

/**
 * Servicio de dominio: decide si una consulta se responde sola o se escala (RF-05, UC-11).
 * Regla central del proyecto: con confianza menor al umbral (60 % por defecto) SIEMPRE se escala.
 */
public final class PoliticaEscalamiento {

    private PoliticaEscalamiento() {
    }

    /**
     * @param resultado salida del motor PLN
     * @param intencion intención de la base de conocimiento con ese nombre (null si no existe)
     * @param umbral    umbral de confianza configurado (0.50 – 0.80)
     * @return el motivo de escalamiento, o vacío si la consulta se puede responder
     */
    public static Optional<MotivoEscalamiento> evaluar(ResultadoClasificacion resultado, Intencion intencion,
                                                       double umbral) {
        if (resultado.esFueraDeDominio() || intencion == null) {
            return Optional.of(MotivoEscalamiento.FUERA_DEL_DOMINIO);
        }
        if (!resultado.confianza().alcanza(umbral)) {
            return Optional.of(MotivoEscalamiento.CONFIANZA_BAJA);
        }
        if (!intencion.tieneRespuestaActiva()) {
            return Optional.of(MotivoEscalamiento.SIN_RESPUESTA_ACTIVA);
        }
        return Optional.empty();
    }
}
