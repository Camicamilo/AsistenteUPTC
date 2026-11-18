#!/usr/bin/env python3
"""Prueba de carga (RNF-05): N usuarios concurrentes conversando con el asistente.

Uso:  python scripts/prueba_carga.py [URL_BASE] [USUARIOS] [CONSULTAS_POR_USUARIO]
      python scripts/prueba_carga.py http://localhost 100 5

Cada usuario abre su sesión anónima y envía varias consultas reales. Al final se informa
el promedio, la mediana, el p95, el máximo y los errores. Criterio de aceptación del documento:
con 100 usuarios concurrentes el tiempo de respuesta no supera los 5 segundos.
Solo usa la librería estándar de Python. También genera tráfico real para el dashboard.
"""
from __future__ import annotations

import json
import random
import statistics
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost").rstrip("/")
USUARIOS = int(sys.argv[2]) if len(sys.argv) > 2 else 100
CONSULTAS = int(sys.argv[3]) if len(sys.argv) > 3 else 5
LIMITE_MS = 5000

PREGUNTAS = [
    "¿Cuándo son las matrículas?", "¿Cómo activo mi correo institucional?", "Olvidé la contraseña del correo",
    "¿Dónde veo el calendario académico?", "¿Cómo pido un certificado de estudio?", "¿Cómo saco el carnet?",
    "¿Cuánto vale la matrícula?", "¿Cómo cancelo una materia?", "No puedo entrar a la plataforma",
    "¿Hay servicio de psicología?", "Soy nuevo, ¿por dónde empiezo?", "¿Qué requisitos piden para entrar?",
    "¿Dónde veo mis notas?", "¿Cómo me conecto al wifi?", "hola", "gracias",
    "¿Quién ganó el partido de ayer?", "Quiero poner una queja",
]


def post(ruta: str, cuerpo: dict):
    req = urllib.request.Request(BASE + ruta, data=json.dumps(cuerpo).encode(), method="POST",
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode())


def usuario(_: int) -> tuple[list[float], int]:
    tiempos, errores = [], 0
    try:
        sid = post("/api/chat/sesiones", {"canal": random.choice(["web", "móvil"])})["sesionId"]
    except Exception:
        return [], CONSULTAS
    for _ in range(CONSULTAS):
        inicio = time.perf_counter()
        try:
            post("/api/chat/consultas", {"sesionId": sid, "texto": random.choice(PREGUNTAS)})
            tiempos.append((time.perf_counter() - inicio) * 1000)
        except (urllib.error.URLError, TimeoutError):
            errores += 1
        time.sleep(random.uniform(0.2, 1.0))  # el estudiante lee antes de volver a escribir
    return tiempos, errores


def main() -> int:
    print(f"Carga: {USUARIOS} usuarios concurrentes × {CONSULTAS} consultas contra {BASE}")
    inicio = time.perf_counter()
    with ThreadPoolExecutor(max_workers=USUARIOS) as ex:
        resultados = list(ex.map(usuario, range(USUARIOS)))
    duracion = time.perf_counter() - inicio
    tiempos = sorted(t for r in resultados for t in r[0])
    errores = sum(r[1] for r in resultados)
    if not tiempos:
        print("No hubo respuestas")
        return 1
    p95 = tiempos[max(0, int(len(tiempos) * 0.95) - 1)]
    print(f"Consultas respondidas: {len(tiempos)}   Errores: {errores}   Duración: {duracion:.1f} s")
    print(f"Promedio: {statistics.mean(tiempos):.0f} ms   Mediana: {statistics.median(tiempos):.0f} ms   "
          f"p95: {p95:.0f} ms   Máximo: {tiempos[-1]:.0f} ms")
    cumple = p95 <= LIMITE_MS and errores == 0
    print(("CUMPLE" if cumple else "NO CUMPLE") + f" RNF-05 (p95 ≤ {LIMITE_MS} ms y sin errores)")
    return 0 if cumple else 1


if __name__ == "__main__":
    sys.exit(main())
