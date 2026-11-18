#!/usr/bin/env python3
"""Prueba de punta a punta del sistema desplegado (frontend + Nginx + backend + PLN + PostgreSQL).

Uso:  python scripts/prueba_e2e.py [URL_BASE]      (por defecto http://localhost)

Recorre lo que hace un estudiante y un administrador real, pasando por Nginx:
  1. El frontend carga.
  2. Se abre una sesión anónima y el motor PLN real clasifica consultas (RF-01 a RF-05).
  3. Una consulta fuera del dominio se escala.
  4. El administrador entra con JWT, ve métricas y crea una intención nueva con ejemplos.
  5. El motor PLN se reentrena solo y el chat empieza a responder la intención nueva (RF-07).
  6. Se mide el tiempo de respuesta de 30 consultas (RNF-01: 95 % en 3 s o menos).
Solo usa la librería estándar de Python.
"""
from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost").rstrip("/")
fallos: list[str] = []


def http(metodo: str, ruta: str, cuerpo=None, token: str | None = None):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(BASE + ruta, data=datos, method=metodo)
    req.add_header("Accept", "application/json")
    if datos is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            texto = r.read().decode()
            return r.status, (json.loads(texto) if texto and r.headers.get_content_type() == "application/json" else texto)
    except urllib.error.HTTPError as e:
        texto = e.read().decode()
        try:
            return e.code, json.loads(texto)
        except ValueError:
            return e.code, texto


def verificar(condicion: bool, descripcion: str) -> None:
    print(("  ✓ " if condicion else "  ✗ ") + descripcion)
    if not condicion:
        fallos.append(descripcion)


def esperar_servicio(segundos: int = 180) -> None:
    limite = time.time() + segundos
    while time.time() < limite:
        try:
            estado, cuerpo = http("GET", "/actuator/health")
            if estado == 200 and cuerpo.get("status") == "UP":
                return
        except Exception:
            pass
        time.sleep(3)
    raise SystemExit("El backend no quedó disponible a tiempo")


def main() -> int:
    print(f"Prueba E2E contra {BASE}")
    esperar_servicio()

    print("1. Frontend")
    estado, html = http("GET", "/")
    verificar(estado == 200 and "<div id=\"root\">" in html, "Nginx entrega la aplicación React")
    estado, _ = http("GET", "/admin/conocimiento")
    verificar(estado == 200, "Las rutas del SPA se resuelven (React Router)")

    print("2. Chat del estudiante con el motor PLN real")
    estado, s = http("POST", "/api/chat/sesiones", {"canal": "web"})
    verificar(estado == 201 and s["sesionId"].startswith("SES-"), "Se crea una sesión anónima")
    sid = s["sesionId"]
    casos = [
        ("¿Cuándo son las matrículas?", "consulta_matricula"),
        ("Olvidé la contraseña del correo", "correo_recuperar_contrasena"),
        ("¿Cómo saco el carnet?", "carnet_estudiantil"),
    ]
    for texto, esperada in casos:
        estado, r = http("POST", "/api/chat/consultas", {"sesionId": sid, "texto": texto})
        verificar(estado == 200 and r.get("tipo") == "respuesta" and r.get("intent") == esperada,
                  f"«{texto}» → {esperada} ({r.get('confianza') if isinstance(r, dict) else r})")

    print("3. Escalamiento")
    estado, r = http("POST", "/api/chat/consultas", {"sesionId": sid, "texto": "¿Quién ganó el partido de fútbol ayer?"})
    verificar(estado == 200 and r.get("tipo") == "escalamiento" and r["dependencia"]["clave"] == "general",
              "Una consulta fuera del dominio se escala a Atención al Ciudadano")
    estado, r = http("POST", "/api/chat/consultas", {"sesionId": sid, "texto": "x" * 501})
    verificar(estado == 400, "Una consulta de 501 caracteres se rechaza (RF-01)")

    print("4. Panel administrativo")
    estado, _ = http("GET", "/api/admin/metricas")
    verificar(estado == 401, "Sin token el panel responde 401")
    estado, login = http("POST", "/api/admin/auth/login", {"correo": "admin@uptc.edu.co", "password": "Admin2026*"})
    verificar(estado == 200 and "token" in login, "El administrador inicia sesión con JWT")
    token = login.get("token")
    estado, m = http("GET", "/api/admin/metricas?rango=7d", token=token)
    verificar(estado == 200 and m["total"] >= 4, f"Las métricas cuentan las consultas (total={m.get('total')})")

    nombre = f"e2e_horario_cafeteria_{int(time.time())}"
    ejemplos = [
        "¿a qué hora abre la cafetería central?",
        "horario de la cafetería central",
        "¿la cafetería central abre los sábados?",
        "¿hasta qué hora atiende la cafetería central?",
        "cafetería central horarios",
    ]
    estado, it = http("POST", "/api/admin/intenciones", {
        "nombre": nombre, "categoria": "administrativo", "descripcion": "Creada por la prueba E2E",
        "dependencia": "bienestar", "respuesta": "La cafetería central atiende en el horario publicado por Bienestar.",
        "ejemplos": ejemplos}, token=token)
    verificar(estado == 201, "Se crea una intención nueva con ejemplos")

    print("5. Reentrenamiento automático del motor PLN")
    respondida = False
    for _ in range(20):
        _, r = http("POST", "/api/chat/consultas", {"sesionId": sid, "texto": "¿a qué hora abre la cafetería central?"})
        if isinstance(r, dict) and r.get("intent") == nombre and r.get("tipo") == "respuesta":
            respondida = True
            break
        time.sleep(1.5)
    verificar(respondida, "El chat responde la intención nueva sin reiniciar nada (RF-07)")
    estado, h = http("GET", f"/api/admin/intenciones/{it.get('id')}/historial", token=token)
    verificar(estado == 200 and h and h[0]["tipoEvento"] == "IntencionCreada", "El cambio quedó en el almacén de eventos")

    print("6. Tiempo de respuesta (RNF-01)")
    tiempos = []
    for i in range(30):
        inicio = time.perf_counter()
        http("POST", "/api/chat/consultas", {"sesionId": sid, "texto": casos[i % 3][0]})
        tiempos.append((time.perf_counter() - inicio) * 1000)
    tiempos.sort()
    p95 = tiempos[int(len(tiempos) * 0.95) - 1]
    verificar(p95 <= 3000, f"p95 = {p95:.0f} ms, promedio = {sum(tiempos) / len(tiempos):.0f} ms (meta ≤ 3000 ms)")

    print()
    if fallos:
        print(f"FALLARON {len(fallos)} verificaciones")
        return 1
    print("Todas las verificaciones pasaron")
    return 0


if __name__ == "__main__":
    sys.exit(main())
