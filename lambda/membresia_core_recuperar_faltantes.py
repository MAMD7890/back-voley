"""
Lambda: galacticos-membresia-recuperar-faltantes
Trigger: MANUAL — ejecutar para corregir membresías faltantes por bug del webhook.

Qué hace:
  - Busca pagos ONLINE PAGADOS de estudiantes AL_DÍA que no tienen
    membresía asociada en membresia_core (pagoOrigen huérfano).
  - Por cada estudiante toma solo el pago más reciente y crea la membresía.
  - Es idempotente: si la membresía ya existe la omite.

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
ENDPOINT = f"{BACKEND_URL}/api/internal/membresias-core/jobs/recuperar-membresias-faltantes"
TIMEOUT_SEG = 120


def lambda_handler(event, context):
    logger.info("Job RecuperarMembresiasFaltantes iniciando...")
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
                "pagosHuerfanos=%s estudiantesEvaluados=%s creadas=%s omitidas=%s errores=%s",
                resultado.get("pagosHuerfanos", "?"),
                resultado.get("estudiantesEvaluados", "?"),
                resultado.get("creadas", "?"),
                resultado.get("omitidas", "?"),
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
