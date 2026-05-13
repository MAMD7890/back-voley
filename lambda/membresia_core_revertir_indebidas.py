"""
Lambda: galacticos-membresia-revertir-indebidas
Trigger: MANUAL — ejecutar DESPUÉS de validar con el preview.

Revierte membresías PAGADA activas creadas indebidamente por el job
recuperar-membresias-faltantes cuando procesó pagos ya cubiertos por la migración.

Por cada membresía indebida:
  1. La cancela (CANCELADA + esActiva=false)
  2. Restaura la membresía FINALIZADA anterior (esActiva=true)
  3. Devuelve el estudiante a EN_MORA

Solo toca membresías cuyo pagoOrigen.fechaPago sea anterior a la fecha indicada,
preservando las membresías creadas por pagos reales del día.

Variable de entorno opcional:
  FECHA  → fecha de ejecución del job a revertir (YYYY-MM-DD, default: hoy)

Variables de entorno requeridas:
  BACKEND_URL       → https://tu-dominio.com  (sin slash final)
  INTERNAL_API_KEY  → clave configurada en app.internal.api-key
"""

import json
import urllib.request
import urllib.error
import os
import logging
from datetime import date

logger = logging.getLogger()
logger.setLevel(logging.INFO)

BACKEND_URL = os.environ["BACKEND_URL"].rstrip("/")
INTERNAL_API_KEY = os.environ["INTERNAL_API_KEY"]


def lambda_handler(event, context):
    fecha = os.environ.get("FECHA") or event.get("fecha") or date.today().isoformat()
    endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/revertir-membresias-indebidas?fecha={fecha}"

    logger.info("Ejecutando reversión para fecha: %s", fecha)
    logger.info("Endpoint: %s", endpoint)

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
        with urllib.request.urlopen(req, timeout=120) as resp:
            body = resp.read().decode("utf-8")
            resultado = json.loads(body)
            logger.info("OK %d: %s", resp.status, body)
            logger.info(
                "indebidas=%s revertidas=%s sinMembresiaAnterior=%s errores=%s",
                resultado.get("indebidas", "?"),
                resultado.get("revertidas", "?"),
                resultado.get("sinMembresiaAnterior", "?"),
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
