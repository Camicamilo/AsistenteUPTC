package co.edu.uptc.asistente.dominio.modelo;

import co.edu.uptc.asistente.dominio.excepcion.ValidacionException;

import java.util.Arrays;

/** Roles del panel administrativo (UC-14). */
public enum Rol {
    SUPERADMINISTRADOR("superadministrador"),
    ADMINISTRADOR("administrador");

    private final String valor;

    Rol(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }

    public static Rol de(String valor) {
        return Arrays.stream(values()).filter(r -> r.valor.equalsIgnoreCase(String.valueOf(valor).strip()))
                .findFirst().orElseThrow(() -> new ValidacionException("rol", "Rol no válido"));
    }
}
