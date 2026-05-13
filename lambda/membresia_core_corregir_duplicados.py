"""
Lambda: galacticos-membresia-core-corregir-duplicados
Trigger: MANUAL — ejecutar una vez para limpiar pagos duplicados que quedaron
         de la migración inicial o de webhooks Wompi procesados dos veces.

Qué hace:
  1. Busca referencias PAY- que aparecen más de una vez en la tabla pago.
  2. Por cada grupo de duplicados: conserva el de menor idPago (canónico),
     redirige cualquier membresia_core que apuntara a los duplicados y los elimina.
  3. Recalcula fechaFin y estadoMembresia de la membresía migrada del estudiante
     afectado usando el conteo correcto de meses.

ADVERTENCIA: Este job elimina filas de la tabla pago. Verificar los
resultados antes y después de ejecutar.

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
ENDPOINT = f"{BACKEND_URL}/api/internal/membresias-core/jobs/corregir-duplicados"
TIMEOUT_SEG = 120


def lambda_handler(event, context):
    logger.info("Corrección de pagos duplicados iniciando...")
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
                "referenciasConDuplicados=%s pagosEliminados=%s "
                "membresiasCorregidas=%s errores=%s",
                resultado.get("referenciasConDuplicados", "?"),
                resultado.get("pagosEliminados", "?"),
                resultado.get("membresiasCorregidas", "?"),
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
