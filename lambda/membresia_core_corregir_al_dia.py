"""
Lambda: galacticos-membresia-corregir-al-dia
Trigger: MANUAL — ejecutar DESPUÉS de validar con el preview.

Crea membresías para estudiantes que están AL_DIA y tienen un pago
registrado, pero no tienen membresía activa en membresia_core.

Caso típico: estudiante registrado y pagado antes de que el webhook
empezara a crear membresías automáticamente.

Por cada afectado:
  1. Busca su pago PAGADO más reciente sin membresía vinculada
  2. Crea la membresía con las fechas calculadas desde ese pago

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
    solo_preview = event.get("preview", False)

    if solo_preview:
        endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/corregir-al-dia-sin-membresia/preview"
        method = "GET"
        data = None
    else:
        endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/corregir-al-dia-sin-membresia"
        method = "POST"
        data = b"{}"

    logger.info("Modo: %s | Endpoint: %s", "PREVIEW" if solo_preview else "EJECUTAR", endpoint)

    req = urllib.request.Request(
        url=endpoint,
        method=method,
        headers={
            "X-Internal-Api-Key": INTERNAL_API_KEY,
            "Content-Type": "application/json",
        },
        data=data,
    )

    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            body = resp.read().decode("utf-8")
            resultado = json.loads(body)

            if solo_preview:
                total = resultado.get("totalAfectados", 0)
                logger.info("Total estudiantes AL_DIA sin membresía: %s", total)
                for item in resultado.get("detalle", []):
                    logger.info(
                        "Estudiante %s (%s) | pago %s del %s | $%s %s | %s mes(es)",
                        item.get("idEstudiante"),
                        item.get("nombreEstudiante"),
                        item.get("pagoOrigen_referencia"),
                        item.get("pagoOrigen_fecha"),
                        item.get("pagoOrigen_valor"),
                        item.get("pagoOrigen_metodo"),
                        item.get("mesesQueAbarcara"),
                    )
            else:
                logger.info(
                    "afectados=%s creadas=%s sinPago=%s errores=%s",
                    resultado.get("afectados", "?"),
                    resultado.get("creadas", "?"),
                    resultado.get("sinPago", "?"),
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
