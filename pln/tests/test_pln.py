"""Pruebas del servicio PLN (unitarias y de la API HTTP)."""
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.entidades import extraer
from app.evaluar import evaluar
from app.main import app
from app.modelo import Clasificador, Ejemplos, leer_nlu_rasa
from app.texto import normalizar

DATA = Path(__file__).resolve().parent.parent / "data"


@pytest.fixture(scope="module")
def clasificador():
    c = Clasificador()
    c.entrenar(leer_nlu_rasa(DATA / "nlu.yml"))
    return c


@pytest.fixture(scope="module")
def cliente(tmp_path_factory):
    import app.main as main

    main.PERSISTENCIA = tmp_path_factory.mktemp("pln") / "entrenamiento.json"
    with TestClient(app) as c:
        yield c


# ---------------------------------------------------------------- texto
def test_normaliza_tildes_mayusculas_y_signos():
    assert normalizar("¿Cuándo son las MATRÍCULAS???") == "cuando son las matriculas"


def test_conserva_la_enie():
    assert normalizar("Contraseña") == "contraseña"


def test_reduce_letras_repetidas():
    assert normalizar("holaaaaa") == "holaa"


# ---------------------------------------------------------------- entidades
def test_extrae_periodo_academico():
    assert {"entity": "periodo", "value": "2027-I", "extractor": "RegexEntityExtractor"} in extraer(
        "matrículas 2027-1"
    )


def test_extrae_tramite_y_fecha():
    tipos = {(e["entity"], e["value"]) for e in extraer("¿Cuándo abren la matrícula del próximo semestre?")}
    assert ("tramite", "matrícula") in tipos
    assert ("fecha", "próximo semestre") in tipos


def test_extrae_correo_con_posicion():
    e = [x for x in extraer("escríbanme a juan@uptc.edu.co") if x["entity"] == "correo"][0]
    assert e["value"] == "juan@uptc.edu.co" and e["start"] == 13


# ---------------------------------------------------------------- modelo
def test_datos_cumplen_rf04(clasificador):
    assert clasificador.info()["intenciones"] >= 31  # 30 temáticas + fallback


def test_clasifica_frase_conocida(clasificador):
    intencion, confianza = clasificador.clasificar("¿Cuándo son las matrículas?")[0]
    assert intencion == "consulta_matricula" and confianza >= 0.6


def test_tolera_errores_de_tipeo(clasificador):
    assert clasificador.clasificar("olvide la contrasena del corrreo")[0][0] == "correo_recuperar_contrasena"


def test_frase_fuera_de_dominio_va_a_fallback(clasificador):
    assert clasificador.clasificar("¿quién ganó el partido de fútbol?")[0][0] == "nlu_fallback"


def test_texto_vacio_va_a_fallback(clasificador):
    assert clasificador.clasificar("   ¿?  ")[0] == ("nlu_fallback", 1.0)


def test_ranking_ordenado_y_suma_menor_o_igual_a_uno(clasificador):
    ranking = clasificador.clasificar("necesito un certificado", top=5)
    confianzas = [c for _, c in ranking]
    assert confianzas == sorted(confianzas, reverse=True) and sum(confianzas) <= 1.0001


def test_rf02_exactitud_en_validacion(clasificador):
    """Criterio de aceptación de RF-02: >= 85 % en el conjunto de validación."""
    assert evaluar(clasificador, 0.6)["exactitud"] >= 0.85


def test_no_entrena_con_una_sola_clase():
    with pytest.raises(ValueError):
        Clasificador().entrenar([Ejemplos("a", ["hola"])])


# ---------------------------------------------------------------- API
def test_health(cliente):
    r = cliente.get("/health")
    assert r.status_code == 200 and r.json()["status"] == "ok"


def test_parse_formato_rasa(cliente):
    r = cliente.post("/model/parse", json={"text": "Olvidé la contraseña del correo"})
    assert r.status_code == 200
    cuerpo = r.json()
    assert cuerpo["intent"]["name"] == "correo_recuperar_contrasena"
    assert 0 <= cuerpo["intent"]["confidence"] <= 1
    assert isinstance(cuerpo["entities"], list) and len(cuerpo["intent_ranking"]) == 5


def test_parse_rechaza_cuerpo_invalido(cliente):
    assert cliente.post("/model/parse", json={}).status_code == 422


def test_train_reemplaza_el_modelo(cliente):
    datos = {
        "nlu": [
            {"intent": "uno", "examples": ["manzana roja", "manzana verde", "fruta manzana"]},
            {"intent": "dos", "examples": ["carro rápido", "carro azul", "vehículo carro"]},
        ]
    }
    r = cliente.post("/model/train", json=datos)
    # 2 intenciones enviadas + nlu_fallback, que se conserva del archivo base
    assert r.status_code == 200 and r.json()["intenciones"] == 3
    assert cliente.post("/model/parse", json={"text": "una manzana"}).json()["intent"]["name"] == "uno"
    # Se restaura el modelo completo para no afectar otras pruebas
    import app.main as main

    main.clasificador.entrenar(leer_nlu_rasa(DATA / "nlu.yml"))


def test_train_valida_nombre_de_intencion(cliente):
    r = cliente.post("/model/train", json={"nlu": [{"intent": "Con Espacios", "examples": ["x"]}]})
    assert r.status_code == 422


def test_train_conserva_fallback(cliente):
    datos = {"nlu": [{"intent": "uno", "examples": ["manzana roja", "manzana verde"]},
                     {"intent": "dos", "examples": ["carro rápido", "carro azul"]}]}
    cliente.post("/model/train", json=datos)
    assert cliente.post("/model/parse", json={"text": "¿quién ganó el partido de fútbol?"}).json()["intent"]["name"] == "nlu_fallback"
    import app.main as main

    main.clasificador.entrenar(leer_nlu_rasa(DATA / "nlu.yml"))
