"""Evalúa el modelo contra data/validacion.yml (criterio de aceptación de RF-02: >= 85 %).

Uso:  python -m app.evaluar [--umbral 0.6]
Imprime la exactitud, cuántos aciertos superan el umbral (se responden solos) y cuántos
errores lo superan (respuestas equivocadas que el estudiante sí vería).
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import yaml

from .modelo import Clasificador, leer_nlu_rasa

DATA = Path(__file__).resolve().parent.parent / "data"


def evaluar(clasificador: Clasificador, umbral: float) -> dict:
    casos = yaml.safe_load((DATA / "validacion.yml").read_text(encoding="utf-8"))
    total = aciertos = aciertos_sobre_umbral = errores_sobre_umbral = 0
    fallos = []
    for esperada, frases in casos.items():
        for frase in frases:
            intencion, confianza = clasificador.clasificar(frase, top=1)[0]
            total += 1
            if intencion == esperada:
                aciertos += 1
                aciertos_sobre_umbral += confianza >= umbral
            else:
                errores_sobre_umbral += confianza >= umbral and intencion != "nlu_fallback"
                fallos.append((frase, esperada, intencion, confianza))
    return {
        "total": total,
        "exactitud": aciertos / total,
        "resueltas_solas": aciertos_sobre_umbral / total,
        "respuestas_equivocadas": errores_sobre_umbral / total,
        "fallos": fallos,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--umbral", type=float, default=0.6)
    args = parser.parse_args()
    c = Clasificador()
    c.entrenar(leer_nlu_rasa(DATA / "nlu.yml"))
    r = evaluar(c, args.umbral)
    print(f"Casos de validación:          {r['total']}")
    print(f"Exactitud (RF-02, meta 85 %): {r['exactitud']:.1%}")
    print(f"Resueltas sin escalar:        {r['resueltas_solas']:.1%}  (confianza >= {args.umbral})")
    print(f"Respuestas equivocadas:       {r['respuestas_equivocadas']:.1%}  (error con confianza >= {args.umbral})")
    for frase, esperada, obtenida, conf in r["fallos"]:
        print(f"  ✗ «{frase}»  esperada={esperada}  obtenida={obtenida} ({conf:.2f})")
    return 0 if r["exactitud"] >= 0.85 else 1


if __name__ == "__main__":
    sys.exit(main())
