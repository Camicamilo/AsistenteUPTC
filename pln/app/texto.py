"""Normalización del texto en español (tildes, mayúsculas, signos y letras repetidas)."""
from __future__ import annotations

import re
import unicodedata

_REPETIDAS = re.compile(r"(.)\1{2,}")
_NO_ALFANUM = re.compile(r"[^a-z0-9ñ@\s]")
_ESPACIOS = re.compile(r"\s+")


def normalizar(texto: str) -> str:
    """'¿Cuándo son las MATRÍCULAS???' -> 'cuando son las matriculas'."""
    t = (texto or "").lower().replace("ñ", "\x00")
    t = unicodedata.normalize("NFD", t)
    t = "".join(c for c in t if unicodedata.category(c) != "Mn").replace("\x00", "ñ")
    t = _NO_ALFANUM.sub(" ", t)
    t = _REPETIDAS.sub(r"\1\1", t)  # "holaaaa" -> "holaa"
    return _ESPACIOS.sub(" ", t).strip()
