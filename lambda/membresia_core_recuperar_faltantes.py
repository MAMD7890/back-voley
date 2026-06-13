"""
Lambda: galacticos-membresia-recuperar-faltantes
Trigger: MANUAL — ejecutar DESPUÉS de validar con el preview.

Busca pagos ONLINE PAGADOS sin membresía asociada en membresia_core (pagoOrigen huérfano).
Por cada estudiante toma el pago más reciente y crea la membresía.
Es idempotente: si la membresía ya existe la omite.

Modos de ejecución:
  {"preview": true}   → solo consulta, sin cambios (muestra qué se crearía)
  {}                  → ejecuta la corrección

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
        endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/recuperar-membresias-faltantes/preview"
        method = "GET"
        data = None
    else:
        endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/recuperar-membresias-faltantes"
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
                logger.info(
                    "Pagos huérfanos: %s | Estudiantes afectados: %s | Se crearán: %s",
                    resultado.get("pagosHuerfanos", 0),
                    resultado.get("estudiantesAfectados", 0),
                    resultado.get("seCrearan", 0),
                )
                for item in resultado.get("detalle_crear", []):
                    nueva = item.get("membresiaQueQuedara", {})
                    logger.info(
                        "CREAR | Estudiante %s (%s) | membresiaActual=%s | "
                        "nuevaMembresia: %s → %s (%s mes(es), %s) | estadoQuedará=%s | origenFecha=%s",
                        item.get("idEstudiante"),
                        item.get("nombreEstudiante"),
                        item.get("membresiaActual"),
                        nueva.get("fechaInicio"),
                        nueva.get("fechaFin"),
                        nueva.get("meses"),
                        nueva.get("estado"),
                        item.get("estadoQueQuedara"),
                        nueva.get("origenFechaInicio"),
                    )
            else:
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
