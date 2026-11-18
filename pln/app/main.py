"""Servicio PLN del Asistente Virtual UPTC.

Expone la MISMA API HTTP que Rasa Open Source 3.x para que el backend (RasaAdapter)
funcione igual contra este servicio liviano o contra un servidor Rasa real:

    GET  /                 información del servicio
    GET  /version          versión (como Rasa)
    GET  /health           estado (200 si el modelo está entrenado, 503 si no)
    POST /model/parse      {"text": "..."} -> intención, confianza, entidades y ranking
    POST /model/train      reentrena con los ejemplos que envía el backend (base de conocimiento)

Variables de entorno:
    PLN_TOKEN         si se define, /model/train exige "Authorization: Bearer <token>" o ?token=<token>
    PLN_DATOS         archivo nlu.yml inicial (por defecto data/nlu.yml)
    PLN_PERSISTENCIA  archivo JSON donde se guarda el último entrenamiento recibido del backend
"""
from __future__ import annotations

import json
import logging
import os
import time
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Header, HTTPException, Query
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from . import __version__
from .entidades import extraer
from .modelo import FALLBACK, Clasificador, Ejemplos, leer_nlu_rasa

log = logging.getLogger("pln")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

BASE = Path(__file__).resolve().parent.parent
DATOS = Path(os.getenv("PLN_DATOS", BASE / "data" / "nlu.yml"))
PERSISTENCIA = Path(os.getenv("PLN_PERSISTENCIA", BASE / "modelos" / "ultimo_entrenamiento.json"))
TOKEN = os.getenv("PLN_TOKEN", "")

clasificador = Clasificador()


def _con_fallback(datos: list[Ejemplos]) -> list[Ejemplos]:
    """El backend no guarda las frases fuera de dominio: si no vienen, se toman del archivo base."""
    if any(d.intencion == FALLBACK for d in datos):
        return datos
    base = [d for d in leer_nlu_rasa(DATOS) if d.intencion == FALLBACK]
    return datos + base


def _cargar_inicial() -> None:
    if PERSISTENCIA.exists():
        try:
            datos = json.loads(PERSISTENCIA.read_text(encoding="utf-8"))
            r = clasificador.entrenar(_con_fallback([Ejemplos(d["intent"], d["examples"]) for d in datos["nlu"]]))
            log.info("Modelo restaurado desde %s: %s", PERSISTENCIA, r)
            return
        except Exception:  # archivo dañado: se entrena con los datos base
            log.exception("No se pudo restaurar %s; se usan los datos base", PERSISTENCIA)
    r = clasificador.entrenar(leer_nlu_rasa(DATOS))
    log.info("Modelo entrenado desde %s: %s", DATOS, r)


@asynccontextmanager
async def ciclo_de_vida(_: FastAPI):
    _cargar_inicial()
    yield


app = FastAPI(
    title="Asistente Virtual UPTC — Servicio PLN",
    version=__version__,
    description="Clasificación de intenciones y extracción de entidades (API compatible con Rasa 3).",
    lifespan=ciclo_de_vida,
)


class ParseRequest(BaseModel):
    text: str = Field(..., max_length=2000)
    message_id: str | None = None


class IntencionEntrenamiento(BaseModel):
    intent: str = Field(..., pattern=r"^[a-z][a-z0-9_]{1,79}$")
    examples: list[str]


class TrainRequest(BaseModel):
    nlu: list[IntencionEntrenamiento]


@app.get("/")
def raiz():
    return {"servicio": "pln-asistente-uptc", "version": __version__, "modelo": clasificador.info()}


@app.get("/version")
def version():
    return {"version": __version__, "minimum_compatible_version": "3.0.0"}


@app.get("/health")
def health():
    if not clasificador.listo:
        return JSONResponse(status_code=503, content={"status": "entrenando"})
    return {"status": "ok", "modelo": clasificador.info()}


@app.post("/model/parse")
def parse(req: ParseRequest):
    inicio = time.perf_counter()
    try:
        ranking = clasificador.clasificar(req.text, top=5)
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    nombre, confianza = ranking[0]
    return {
        "text": req.text,
        "intent": {"name": nombre, "confidence": confianza},
        "entities": extraer(req.text),
        "intent_ranking": [{"name": n, "confidence": c} for n, c in ranking],
        "tiempo_ms": round((time.perf_counter() - inicio) * 1000, 2),
    }


@app.post("/model/train")
def train(req: TrainRequest, authorization: str | None = Header(default=None), token: str | None = Query(default=None)):
    if TOKEN and authorization != f"Bearer {TOKEN}" and token != TOKEN:
        raise HTTPException(status_code=401, detail="Token inválido")
    datos = [Ejemplos(i.intent, [e for e in i.examples if e.strip()]) for i in req.nlu]
    datos = [d for d in datos if d.textos]
    try:
        r = clasificador.entrenar(_con_fallback(datos))
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e)) from e
    try:
        PERSISTENCIA.parent.mkdir(parents=True, exist_ok=True)
        PERSISTENCIA.write_text(json.dumps(req.model_dump(), ensure_ascii=False), encoding="utf-8")
    except OSError:
        log.warning("No se pudo persistir el entrenamiento en %s", PERSISTENCIA)
    log.info("Reentrenado desde el backend: %s", r)
    return r
