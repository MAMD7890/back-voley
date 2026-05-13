"""
Lambda: galacticos-membresia-core-job5-sincronizar-estados
Trigger: MANUAL — ejecutar una sola vez después de la migración, o cuando
         se sospeche que los estados de los estudiantes están desincronizados.

Job 5 — SincronizarEstados
Recorre todas las membresías con esActiva = true y actualiza el estadoPago
del estudiante para que coincida con el estado real de su membresía:

  PAGADA                        → AL_DIA
  PENDIENTE_PAGO (ACUERDO_PAGO) → COMPROMISO_PAGO
  PENDIENTE_PAGO (otro tipo)    → PENDIENTE
  EN_MORA / FINALIZADA /
  COMPROMISO_INCUMPLIDO /
  CANCELADA                     → EN_MORA

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
ENDPOINT = f"{BACKEND_URL}/api/internal/membresias-core/jobs/sincronizar-estados"
TIMEOUT_SEG = 120


def lambda_handler(event, context):
    logger.info("Job 5 — SincronizarEstados iniciando...")
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
                "evaluados=%s actualizados=%s sinCambio=%s errores=%s",
                resultado.get("evaluados", "?"),
                resultado.get("actualizados", "?"),
                resultado.get("sinCambio", "?"),
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
