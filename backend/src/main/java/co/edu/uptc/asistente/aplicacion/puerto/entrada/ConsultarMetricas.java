package co.edu.uptc.asistente.aplicacion.puerto.entrada;

import co.edu.uptc.asistente.aplicacion.dto.FiltroHistorial;
import co.edu.uptc.asistente.aplicacion.dto.Metricas;
import co.edu.uptc.asistente.aplicacion.dto.Pagina;
import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;

/** Métricas e historial del panel (UC-15). */
public interface ConsultarMetricas {
    Metricas calcular(String rango);

    Pagina<RegistroConsulta> historial(FiltroHistorial filtro);

    /** Exportación del historial en CSV (separado por punto y coma, abre bien en Excel en español). */
    String exportarCsv(String rango);
}
