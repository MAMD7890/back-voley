"""
Lambda: galacticos-membresia-revertir-indebidas-preview
Trigger: MANUAL — ejecutar ANTES de revertir para validar qué cambiará.

Muestra sin hacer cambios:
  - Qué membresías PAGADA activas serán canceladas
  - Qué membresía FINALIZADA se restaurará por estudiante
  - Estado actual del estudiante y estado que quedará tras la reversión

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
    endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/revertir-membresias-indebidas/preview?fecha={fecha}"

    logger.info("Preview reversión para fecha: %s", fecha)
    logger.info("Endpoint: %s", endpoint)

    req = urllib.request.Request(
        url=endpoint,
        method="GET",
        headers={
            "X-Internal-Api-Key": INTERNAL_API_KEY,
            "Content-Type": "application/json",
        },
    )

    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = resp.read().decode("utf-8")
            resultado = json.loads(body)
            total = resultado.get("totalIndebidas", 0)
            logger.info("Total membresías a revertir: %s", total)

            for item in resultado.get("detalle", []):
                logger.info(
                    "Estudiante %s (%s) | estado actual: %s → quedará: %s | "
                    "membresía indebida %s→%s | pago origen %s (%s) | "
                    "restaurar membresía anterior: %s",
                    item.get("idEstudiante"),
                    item.get("nombreEstudiante"),
                    item.get("estadoActualEstudiante"),
                    item.get("estadoQueQuedara"),
                    item.get("membresiaIndebida_fechaInicio"),
                    item.get("membresiaIndebida_fechaFin"),
                    item.get("pagoOrigen_referencia"),
                    item.get("pagoOrigen_fecha"),
                    item.get("hayMembresiaAnterior"),
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
