"""Clasificador de intenciones.

Pipeline (equivalente liviano al de Rasa con CountVectors + DIET):
  1. normalización del texto (tildes, mayúsculas, letras repetidas)
  2. TF-IDF de palabras (1-2 gramas)  -> captura el vocabulario de cada intención
  3. TF-IDF de caracteres (2-5 gramas) -> tolera errores de tipeo ("matricla", "carnet"/"carné")
  4. regresión logística multinomial   -> devuelve una probabilidad por intención (la "confianza")

La clase especial "nlu_fallback" se entrena con frases fuera del dominio, igual que en Rasa.
"""
from __future__ import annotations

import threading
import time
from dataclasses import dataclass, field
from pathlib import Path

import yaml
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import FeatureUnion, Pipeline

from .texto import normalizar

FALLBACK = "nlu_fallback"


@dataclass
class Ejemplos:
    intencion: str
    textos: list[str] = field(default_factory=list)


def leer_nlu_rasa(ruta: Path) -> list[Ejemplos]:
    """Lee un archivo nlu.yml en formato Rasa 3."""
    datos = yaml.safe_load(ruta.read_text(encoding="utf-8")) or {}
    resultado = []
    for bloque in datos.get("nlu", []):
        if "intent" not in bloque:
            continue
        textos = [
            linea.strip()[2:].strip()
            for linea in str(bloque.get("examples", "")).splitlines()
            if linea.strip().startswith("- ")
        ]
        resultado.append(Ejemplos(bloque["intent"], textos))
    return resultado


def _pipeline() -> Pipeline:
    return Pipeline(
        [
            (
                "rasgos",
                FeatureUnion(
                    [
                        ("palabras", TfidfVectorizer(preprocessor=normalizar, ngram_range=(1, 2), sublinear_tf=True)),
                        (
                            "caracteres",
                            TfidfVectorizer(
                                preprocessor=normalizar, analyzer="char_wb", ngram_range=(2, 5), sublinear_tf=True
                            ),
                        ),
                    ]
                ),
            ),
            ("clasificador", LogisticRegression(C=100, max_iter=3000)),
        ]
    )


class Clasificador:
    """Envuelve el modelo entrenado. El reentrenamiento es atómico: se entrena uno nuevo y se reemplaza."""

    def __init__(self) -> None:
        self._modelo: Pipeline | None = None
        self._intenciones: list[str] = []
        self._ejemplos = 0
        self._entrenado_en: float | None = None
        self._lock = threading.Lock()

    @property
    def listo(self) -> bool:
        return self._modelo is not None

    def info(self) -> dict:
        return {
            "intenciones": len(self._intenciones),
            "ejemplos": self._ejemplos,
            "entrenado_en": self._entrenado_en,
        }

    def entrenar(self, datos: list[Ejemplos]) -> dict:
        x, y = [], []
        for d in datos:
            for t in d.textos:
                if t.strip():
                    x.append(t)
                    y.append(d.intencion)
        clases = sorted(set(y))
        if len(clases) < 2:
            raise ValueError("Se necesitan al menos dos intenciones con ejemplos para entrenar")
        inicio = time.perf_counter()
        modelo = _pipeline().fit(x, y)
        with self._lock:
            self._modelo = modelo
            self._intenciones = clases
            self._ejemplos = len(x)
            self._entrenado_en = time.time()
        return {**self.info(), "segundos": round(time.perf_counter() - inicio, 3)}

    def clasificar(self, texto: str, top: int = 5) -> list[tuple[str, float]]:
        """Devuelve el ranking [(intención, confianza)] ordenado de mayor a menor."""
        with self._lock:
            modelo = self._modelo
        if modelo is None:
            raise RuntimeError("El modelo no está entrenado")
        if not normalizar(texto):
            return [(FALLBACK, 1.0)]
        probabilidades = modelo.predict_proba([texto])[0]
        ranking = sorted(zip(modelo.classes_, probabilidades), key=lambda p: p[1], reverse=True)
        return [(str(i), round(float(p), 4)) for i, p in ranking[:top]]
