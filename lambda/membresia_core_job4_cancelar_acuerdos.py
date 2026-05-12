"""
Lambda: galacticos-membresia-core-job4-cancelar-acuerdos
Trigger: EventBridge cron(0 5 * * ? *) → medianoche hora Colombia (UTC-5)

Job 4 — CancelarAcuerdosVencidos
Cancela acuerdos de pago cuyo fechaLimiteCompromiso venció sin pago.
- DESDE_PENDIENTE: estudiante inactivo directo → SIN_MEMBRESIA
- DESDE_MORA: estudiante sigue activo, se crea membresía MORA con 15 días de gracia

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
ENDPOINT = f"{BACKEND_URL}/api/internal/membresias-core/jobs/cancelar-acuerdos"
TIMEOUT_SEG = 60


def lambda_handler(event, context):
    logger.info("Job 4 — CancelarAcuerdosVencidos iniciando...")
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
                "canceladas=%s morasCreadas=%s errores=%s",
                resultado.get("canceladas", "?"),
                resultado.get("morasCreadas", "?"),
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
