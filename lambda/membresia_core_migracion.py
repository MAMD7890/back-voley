"""
Lambda: galacticos-membresia-core-migracion
Trigger: MANUAL — ejecutar una sola vez durante la migración

Migra los registros existentes de la tabla `membresia` a `membresia_core`.
Solo migra membresías activas o cuya fechaFin sea < 1 mes atrás.
Los demás estudiantes quedan con estadoPago = SIN_MEMBRESIA.

ADVERTENCIA: Este job es idempotente en la lectura pero inserta nuevos registros.
Ejecutar una sola vez o verificar duplicados antes de re-ejecutar.

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
ENDPOINT = f"{BACKEND_URL}/api/internal/membresias-core/migracion"
TIMEOUT_SEG = 300  # migración puede tomar varios minutos


def lambda_handler(event, context):
    logger.info("Migración membresia_core iniciando...")
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
                "migradas=%s omitidas=%s sinMembresia=%s errores=%s",
                resultado.get("membresiasMigradas", "?"),
                resultado.get("membresiasOmitidas", "?"),
                resultado.get("estudiantesSinMembresia", "?"),
                resultado.get("errores", "?"),
            )
            return {"statusCode": 200, "body": resultado}

    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        logger.error("HTTP %d: %s", e.code, body)
        raise RuntimeError(f"Backend respondió {e.code}: {body}")

    except urllib.error.URLError as e:
        logger.error("Conexión fallida: %s", str(e.reason))
        raise RuntimeError(f"Error de conexión: {e.reason}")

    except Exception as e:
        logger.error("Error inesperado: %s", str(e))
        raise
