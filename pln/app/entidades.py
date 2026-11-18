"""Extractor de entidades por expresiones regulares (equivale al RegexEntityExtractor de Rasa).

Entidades que reconoce:
  - periodo:  "2027-1", "2027 I", "2026-II"   -> "2027-I"
  - tramite:  matrícula, certificado, carné, reintegro, cancelación, inscripción, homologación, grado
  - fecha:    "próximo semestre", "este semestre", "mañana", "hoy"
  - correo:   cualquier dirección de correo escrita en el mensaje
"""
from __future__ import annotations

import re

from .texto import normalizar

_PERIODO = re.compile(r"\b(20\d{2})\s*[-–]?\s*(ii|i|2|1)\b")
_CORREO = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+")
_TRAMITES = {
    "matricula": "matrícula",
    "certificado": "certificado",
    "constancia": "certificado",
    "carnet": "carné",
    "carne": "carné",
    "reintegro": "reintegro",
    "cancelacion": "cancelación",
    "cancelar": "cancelación",
    "inscripcion": "inscripción",
    "homolog": "homologación",
    "grado": "grado",
}
_FECHAS = {
    "proximo semestre": "próximo semestre",
    "siguiente semestre": "próximo semestre",
    "otro semestre": "próximo semestre",
    "este semestre": "semestre actual",
    "mañana": "mañana",
    "hoy": "hoy",
}


def extraer(texto: str) -> list[dict]:
    entidades: list[dict] = []
    for m in _CORREO.finditer(texto or ""):
        entidades.append(_entidad("correo", m.group(0), m.start(), m.end()))

    n = normalizar(texto)
    m = _PERIODO.search(n)
    if m:
        romano = "II" if m.group(2) in ("ii", "2") else "I"
        entidades.append(_entidad("periodo", f"{m.group(1)}-{romano}"))

    vistos = set()
    for palabra in n.split():
        for raiz, valor in _TRAMITES.items():
            if palabra.startswith(raiz) and valor not in vistos:
                vistos.add(valor)
                entidades.append(_entidad("tramite", valor))

    for frase, valor in _FECHAS.items():
        if re.search(rf"\b{frase}\b", n):
            entidades.append(_entidad("fecha", valor))
            break
    return entidades


def _entidad(tipo: str, valor: str, inicio: int | None = None, fin: int | None = None) -> dict:
    e = {"entity": tipo, "value": valor, "extractor": "RegexEntityExtractor"}
    if inicio is not None:
        e["start"], e["end"] = inicio, fin
    return e
