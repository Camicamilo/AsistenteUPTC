# Servicio PLN (API compatible con Rasa) — imagen liviana
FROM python:3.11-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 \
    PLN_PERSISTENCIA=/app/modelos/ultimo_entrenamiento.json
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app ./app
COPY data ./data
RUN useradd --create-home pln && mkdir -p /app/modelos && chown pln /app/modelos
USER pln
EXPOSE 5005
HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=5 \
  CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:5005/health').status==200 else 1)"
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-5005}"]
