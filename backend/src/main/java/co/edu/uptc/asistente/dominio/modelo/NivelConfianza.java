package co.edu.uptc.asistente.dominio.modelo;

import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;

/** Objeto de valor: qué tan seguro está el motor PLN de la intención detectada (0.00 a 1.00). */
public record NivelConfianza(double valor) {
    public NivelConfianza {
        if (Double.isNaN(valor) || valor < 0 || valor > 1) {
            throw new ValidacionException("confianza", "La confianza debe estar entre 0 y 1");
        }
    }

    public static NivelConfianza de(double valor) {
        return new NivelConfianza(Math.max(0, Math.min(1, valor)));
    }

    public boolean alcanza(double umbral) {
        return valor >= umbral;
    }

    /** Valor redondeado a dos decimales, como se muestra y se guarda. */
    public double redondeado() {
        return Math.round(valor * 100) / 100.0;
    }
}
