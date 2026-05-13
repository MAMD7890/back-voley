"""
Lambda: galacticos-membresia-core-mantenimiento
Trigger: MANUAL — ejecutar cuando se sospeche desincronización o tras la migración inicial.

Ejecuta en orden:
  1. corregir-duplicados  → elimina pagos PAY- duplicados y recalcula membresías afectadas
  2. sincronizar-estados  → alinea estadoPago del estudiante con su membresía activa
                            y corrige tipoMembresia mal asignado (EFECTIVO vs ONLINE)

Variables de entorno requeridas:
  BACKEND_URL       → https://tu-dominio.com  (sin slash final)
  INTERNAL_API_KEY  → clave configurada en app.internal.api-key
"""

import json
import urllib.request
import urllib.error
import os
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

BACKEND_URL = os.environ["BACKEND_URL"].rstrip("/")
INTERNAL_API_KEY = os.environ["INTERNAL_API_KEY"]
TIMEOUT_SEG = 120

JOBS = [
    {
        "nombre": "corregir-duplicados",
        "endpoint": "/api/internal/membresias-core/jobs/corregir-duplicados",
    },
    {
        "nombre": "sincronizar-estados",
        "endpoint": "/api/internal/membresias-core/jobs/sincronizar-estados",
    },
]


def llamar_endpoint(endpoint):
    url = f"{BACKEND_URL}{endpoint}"
    req = urllib.request.Request(
        url=url,
        method="POST",
        headers={
            "X-Internal-Api-Key": INTERNAL_API_KEY,
            "Content-Type": "application/json",
        },
        data=b"{}",
    )
    with urllib.request.urlopen(req, timeout=TIMEOUT_SEG) as resp:
        return resp.status, json.loads(resp.read().decode("utf-8"))


def lambda_handler(event, context):
    logger.info("Mantenimiento membresia_core iniciando...")

    resultados = {}

    for job in JOBS:
        nombre = job["nombre"]
        logger.info("▶ Ejecutando: %s", nombre)
        try:
            status, resultado = llamar_endpoint(job["endpoint"])
            logger.info("✅ %s OK %d: %s", nombre, status, resultado)
            resultados[nombre] = {"ok": True, "resultado": resultado}
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8")
            logger.error("❌ %s HTTP %d: %s", nombre, e.code, body)
            resultados[nombre] = {"ok": False, "error": f"HTTP {e.code}: {body}"}
        except Exception as e:
            logger.error("❌ %s error: %s", nombre, str(e))
            resultados[nombre] = {"ok": False, "error": str(e)}

    logger.info("Mantenimiento finalizado: %s", resultados)
    return {"statusCode": 200, "body": resultados}
