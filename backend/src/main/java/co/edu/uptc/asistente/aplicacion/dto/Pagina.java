package co.edu.uptc.asistente.aplicacion.dto;

import java.util.List;
import java.util.function.Function;

/** Resultado paginado (lo consume la tabla del panel). */
public record Pagina<T>(List<T> items, long total, int page, int pageSize, int paginas) {

    public static <T> Pagina<T> de(List<T> lista, int page, int pageSize) {
        int tamano = Math.max(1, Math.min(pageSize, 100));
        int totalPaginas = Math.max(1, (int) Math.ceil(lista.size() / (double) tamano));
        int p = Math.min(Math.max(1, page), totalPaginas);
        int desde = Math.min((p - 1) * tamano, lista.size());
        int hasta = Math.min(desde + tamano, lista.size());
        return new Pagina<>(List.copyOf(lista.subList(desde, hasta)), lista.size(), p, tamano, totalPaginas);
    }

    public <R> Pagina<R> map(Function<T, R> f) {
        return new Pagina<>(items.stream().map(f).toList(), total, page, pageSize, paginas);
    }
}
