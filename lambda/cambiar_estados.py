"""
Lambda: galacticos-actualizar-estados
Trigger: EventBridge cron(0 5 * * ? *) → medianoche hora Colombia (UTC-5)

Llama al endpoint interno del backend para actualizar estudiantes a EN_MORA
cuando su membresía ya venció.

Variables de entorno requeridas en Lambda:
  BACKEND_URL     → https://tu-dominio.com  (sin slash final)
  INTERNAL_API_KEY → la misma clave configurada en app.internal.api-key
"""

import json
import urllib.request
import urllib.error
import os
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

BACKEND_URL    = os.environ["BACKEND_URL"].rstrip("/")
INTERNAL_API_KEY = os.environ["INTERNAL_API_KEY"]
ENDPOINT       = f"{BACKEND_URL}/api/internal/estados/actualizar"
TIMEOUT_SEG    = 30


def lambda_handler(event, context):
    logger.info("🌙 Iniciando actualización de estados de membresía...")
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
            body     = resp.read().decode("utf-8")
            status   = resp.status
            resultado = json.loads(body)

            logger.info("✅ Respuesta %d: %s", status, body)
            logger.info(
                "📊 Actualizados: %s | Omitidos: %s | Errores: %s",
                resultado.get("actualizados", "?"),
                resultado.get("omitidos", "?"),
                resultado.get("errores", "?"),
            )

            return {
                "statusCode": 200,
                "body": resultado,
            }

    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        logger.error("❌ HTTP %d: %s", e.code, body)
        raise RuntimeError(f"Backend respondió {e.code}: {body}")

    except urllib.error.URLError as e:
        logger.error("❌ No se pudo conectar al backend: %s", str(e.reason))
        raise RuntimeError(f"Error de conexión: {e.reason}")

    except Exception as e:
        logger.error("❌ Error inesperado: %s", str(e))
        raise
