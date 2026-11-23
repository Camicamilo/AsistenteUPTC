package co.edu.uptc.asistente.infraestructura.entrada.rest;

import co.edu.uptc.asistente.aplicacion.dto.FiltroHistorial;
import co.edu.uptc.asistente.aplicacion.dto.Metricas;
import co.edu.uptc.asistente.aplicacion.dto.Pagina;
import co.edu.uptc.asistente.aplicacion.puerto.entrada.ConsultarMetricas;
import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Métricas e historial (UC-15) — MetricsDashboard. */
@Tag(name = "Métricas", description = "Indicadores, historial y exportación (UC-15)")
@RestController
@RequestMapping("/api/admin")
public class MetricasController {
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ConsultarMetricas metricas;

    public MetricasController(ConsultarMetricas metricas) {
        this.metricas = metricas;
    }

    @Operation(summary = "Indicadores del dashboard. rango: hoy, 7d, 30d o 90d")
    @GetMapping("/metricas")
    public Metricas metricas(@RequestParam(defaultValue = "7d") String rango) {
        return metricas.calcular(rango);
    }

    @Operation(summary = "Historial de consultas. estado: resuelta o escalada")
    @GetMapping("/consultas")
    public Pagina<RegistroConsulta> historial(@RequestParam(defaultValue = "7d") String rango,
                                              @RequestParam(required = false) String estado,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "15") int pageSize) {
        return metricas.historial(new FiltroHistorial(rango, estado, q, page, pageSize));
    }

    @Operation(summary = "Exporta el historial en CSV (Excel)")
    @GetMapping("/consultas/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(defaultValue = "30d") String rango) {
        // BOM para que Excel reconozca las tildes
        byte[] cuerpo = ("﻿" + metricas.exportarCsv(rango)).getBytes(StandardCharsets.UTF_8);
        String archivo = "consultas-asistente-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                .body(cuerpo);
    }
}
