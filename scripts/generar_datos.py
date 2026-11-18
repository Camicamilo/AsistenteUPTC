#!/usr/bin/env python3
"""Genera los datos derivados de datos/base_conocimiento.yml.

Salidas:
  - pln/data/nlu.yml                                      entrenamiento del motor PLN (formato Rasa 3.1)
  - backend/src/main/resources/db/migration/V2__semilla.sql  semilla de la base de datos (Flyway)

Uso:
    pip install pyyaml bcrypt
    python scripts/generar_datos.py

Las contraseñas de los usuarios de demostración se guardan con BCrypt (nunca en texto plano).
Cámbialas desde el panel apenas el sistema quede desplegado.
"""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path

import bcrypt
import yaml

RAIZ = Path(__file__).resolve().parent.parent
FUENTE = RAIZ / "datos" / "base_conocimiento.yml"
SALIDA_NLU = RAIZ / "pln" / "data" / "nlu.yml"
SALIDA_SQL = RAIZ / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V2__semilla.sql"

USUARIOS_DEMO = [
    ("Administrador General", "admin@uptc.edu.co", "Admin2026*", "superadministrador"),
    ("Gestor de Conocimiento", "gestor@uptc.edu.co", "Gestor2026*", "administrador"),
]

CONFIG_DEFECTO = {
    "umbral": 0.60,
    "inactividad": 30,
    "plantilla": "No tengo información suficiente para responder esa pregunta con seguridad. "
    "Te recomiendo comunicarte con {dependencia}.",
    "notificar": True,
}

CATEGORIAS = {"académico", "administrativo", "técnico"}


def _sal(correo: str) -> bytes:
    """Sal fija por usuario para que el archivo generado sea reproducible (los datos son de demostración)."""
    alfabeto = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    digest = hashlib.sha256(correo.encode()).digest()
    cuerpo = "".join(alfabeto[b % 64] for b in digest[:21]) + ".Oeu"[digest[21] % 4]  # el último carácter solo admite 4 valores
    return ("$2b$10$" + cuerpo).encode()


def sql(texto: str | None) -> str:
    if texto is None:
        return "NULL"
    return "'" + str(texto).replace("'", "''") + "'"


def validar(datos: dict) -> None:
    claves = {d["clave"] for d in datos["dependencias"]}
    nombres = set()
    for it in datos["intenciones"]:
        n = it["nombre"]
        assert n not in nombres, f"intención repetida: {n}"
        nombres.add(n)
        assert it["categoria"] in CATEGORIAS, f"{n}: categoría inválida"
        assert it["dependencia"] in claves, f"{n}: dependencia desconocida"
        assert len(it["ejemplos"]) >= 8, f"{n}: necesita al menos 8 ejemplos"
        assert it["respuestas"], f"{n}: sin respuestas"
        for r in it["respuestas"]:
            assert len(r) <= 1500, f"{n}: respuesta de más de 1500 caracteres"
    tematicas = [i for i in datos["intenciones"] if not i.get("social")]
    assert len(tematicas) >= 30, "RF-04 exige al menos 30 intenciones temáticas"


def generar_nlu(datos: dict) -> str:
    lineas = [
        "# ARCHIVO GENERADO por scripts/generar_datos.py — no editar a mano.",
        "# Formato Rasa 3: sirve tanto para el servicio PLN liviano como para `rasa train nlu`.",
        'version: "3.1"',
        "nlu:",
    ]
    for it in datos["intenciones"]:
        lineas.append(f"- intent: {it['nombre']}")
        lineas.append("  examples: |")
        lineas += [f"    - {e}" for e in it["ejemplos"]]
    lineas.append("- intent: nlu_fallback")
    lineas.append("  examples: |")
    lineas += [f"    - {e}" for e in datos["fuera_de_dominio"]]
    return "\n".join(lineas) + "\n"


def generar_sql(datos: dict) -> str:
    s = [
        "-- ARCHIVO GENERADO por scripts/generar_datos.py a partir de datos/base_conocimiento.yml.",
        "-- Contenido de DEMOSTRACIÓN: el enlace institucional debe validarlo (RD-01, RD-03, RD-04).",
        "",
        "-- Dependencias (datos de contacto para el escalamiento, UC-11)",
    ]
    for d in datos["dependencias"]:
        s.append(
            "INSERT INTO dependencias (clave, nombre, correo, telefono, enlace, horario) VALUES ("
            + ", ".join(sql(d[k]) for k in ("clave", "nombre", "correo", "telefono", "enlace", "horario"))
            + ");"
        )

    s += ["", "-- Usuarios del panel (contraseñas con BCrypt)"]
    for nombre, correo, clave, rol in USUARIOS_DEMO:
        h = bcrypt.hashpw(clave.encode(), _sal(correo)).decode()
        s.append(
            "INSERT INTO usuarios (nombre, correo, password_hash, rol, activo, debe_cambiar_password, fecha_registro) "
            f"VALUES ({sql(nombre)}, {sql(correo)}, {sql(h)}, {sql(rol)}, TRUE, FALSE, CURRENT_TIMESTAMP);"
        )

    c = CONFIG_DEFECTO
    s += [
        "",
        "-- Configuración del sistema (UC-16)",
        "INSERT INTO configuracion (id, umbral_confianza, inactividad_sesion_min, plantilla_escalamiento, notificar_por_correo) "
        f"VALUES (1, {c['umbral']:.2f}, {c['inactividad']}, {sql(c['plantilla'])}, {'TRUE' if c['notificar'] else 'FALSE'});",
        "",
        "-- Temas de acceso rápido del widget",
    ]
    for orden, t in enumerate(datos["temas_rapidos"], start=1):
        s.append(
            "INSERT INTO temas_rapidos (tema_id, etiqueta, consulta, orden) VALUES ("
            f"{sql(t['id'])}, {sql(t['etiqueta'])}, {sql(t['consulta'])}, {orden});"
        )

    s += ["", "-- Intenciones, ejemplos de entrenamiento y respuestas"]
    for it in datos["intenciones"]:
        n = it["nombre"]
        sub = f"(SELECT intent_id FROM intenciones WHERE nombre_intent = {sql(n)})"
        s.append(
            "INSERT INTO intenciones (nombre_intent, descripcion, categoria, dependencia, social) VALUES ("
            f"{sql(n)}, {sql(it['descripcion'])}, {sql(it['categoria'])}, {sql(it['dependencia'])}, "
            f"{'TRUE' if it.get('social') else 'FALSE'});"
        )
        for e in it["ejemplos"]:
            s.append(f"INSERT INTO ejemplos_intencion (intent_id, texto) VALUES ({sub}, {sql(e)});")
        for r in it["respuestas"]:
            s.append(
                "INSERT INTO respuestas (intent_id, texto_respuesta, activa, fecha_actualizacion, actualizado_por) "
                f"VALUES ({sub}, {sql(r)}, TRUE, CURRENT_TIMESTAMP, NULL);"
            )
    return "\n".join(s) + "\n"


def main() -> int:
    datos = yaml.safe_load(FUENTE.read_text(encoding="utf-8"))
    validar(datos)
    SALIDA_NLU.parent.mkdir(parents=True, exist_ok=True)
    SALIDA_SQL.parent.mkdir(parents=True, exist_ok=True)
    SALIDA_NLU.write_text(generar_nlu(datos), encoding="utf-8")
    SALIDA_SQL.write_text(generar_sql(datos), encoding="utf-8")
    total = len(datos["intenciones"])
    ejemplos = sum(len(i["ejemplos"]) for i in datos["intenciones"])
    print(f"OK: {total} intenciones, {ejemplos} ejemplos -> {SALIDA_NLU.relative_to(RAIZ)}, {SALIDA_SQL.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
