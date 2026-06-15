"""
Lambda: galacticos-asistencia-migrar-legado
Trigger: MANUAL — ejecutar UNA SOLA VEZ para migrar registros del modelo antiguo al v2.

Migra todos los registros de asistencia_estudiante → sesion_asistencia + asistencia_v2.
Es idempotente: omite registros que ya existan en v2.

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


def lambda_handler(event, context):
    endpoint = f"{BACKEND_URL}/api/internal/asistencia/jobs/migrar-legado"

    logger.info("Iniciando migración de asistencias legado → v2 | Endpoint: %s", endpoint)

    req = urllib.request.Request(
        url=endpoint,
        method="POST",
        headers={
            "X-Internal-Api-Key": INTERNAL_API_KEY,
            "Content-Type": "application/json",
        },
        data=b"{}",
    )

    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            body = resp.read().decode("utf-8")
            resultado = json.loads(body)

            logger.info(
                "Migración completada | totalLegado=%s | sesionesCreadas=%s | "
                "registrosCreados=%s | registrosOmitidos=%s | errores=%s",
                resultado.get("totalLegado", "?"),
                resultado.get("sesionesCreadas", "?"),
                resultado.get("registrosCreados", "?"),
                resultado.get("registrosOmitidos", "?"),
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
