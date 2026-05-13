"""
Lambda: galacticos-membresia-core-migrar-acuerdos
Trigger: MANUAL — ejecutar una vez después de la migración inicial para crear
         los registros ACUERDO_PAGO de estudiantes que ya tenían COMPROMISO_PAGO.

Qué hace:
  - Busca estudiantes con estadoPago = COMPROMISO_PAGO que no tienen membresía
    ACUERDO_PAGO activa en membresia_core.
  - Por cada uno crea el registro ACUERDO_PAGO con las fechas calculadas desde
    la última membresía activa, y toma observacionPago y fechaLimiteCompromiso
    directamente del estudiante.
  - Es idempotente: si el acuerdo ya existe lo omite.

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
ENDPOINT = f"{BACKEND_URL}/api/internal/membresias-core/jobs/migrar-acuerdos"
TIMEOUT_SEG = 120


def lambda_handler(event, context):
    logger.info("Migracion de acuerdos de pago iniciando...")
    logger.info("Endpoint: %s", ENDPOINT)

    req = urllib.request.Request(
        url=ENDPOINT,
        method="POST",
        headers={
            "X-Internal-Api-Key": INTERNAL_API_KEY,
            "Content-Type": "application/json",
        },
        data=b"{}",
    )

    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT_SEG) as resp:
            body = resp.read().decode("utf-8")
            resultado = json.loads(body)
            logger.info("OK %d: %s", resp.status, body)
            logger.info(
                "creados=%s omitidos=%s errores=%s",
                resultado.get("creados", "?"),
                resultado.get("omitidos", "?"),
                resultado.get("errores", "?"),
            )
            return {"statusCode": 200, "body": resultado}

    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        logger.error("HTTP %d: %s", e.code, body)
        raise RuntimeError(f"Backend respondio {e.code}: {body}")

    except urllib.error.URLError as e:
        logger.error("Conexion fallida: %s", str(e.reason))
        raise RuntimeError(f"Error de conexion: {e.reason}")

    except Exception as e:
        logger.error("Error inesperado: %s", str(e))
        raise
