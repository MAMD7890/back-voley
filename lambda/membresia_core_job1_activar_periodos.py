"""
Lambda: galacticos-membresia-core-job1-activar-periodos
Trigger: EventBridge cron(0 5 * * ? *) → medianoche hora Colombia (UTC-5)

Job 1 — ActivarPeriodosFuturos
Busca membresías PAGADA cuya fechaInicio es hoy (o que quedaron sin activar),
marca la membresía anterior del estudiante como FINALIZADA y confirma estadoPago = AL_DIA.

Modos de ejecución:
  {"preview": true}   → solo consulta, sin cambios (muestra qué se procesaría)
  {}                  → ejecuta el job

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
TIMEOUT_SEG = 60


def lambda_handler(event, context):
    solo_preview = event.get("preview", False)

    if solo_preview:
        endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/activar-periodos/preview"
        method = "GET"
        data = None
    else:
        endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/activar-periodos"
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
        with urllib.request.urlopen(req, timeout=TIMEOUT_SEG) as resp:
            body = resp.read().decode("utf-8")
            resultado = json.loads(body)

            if solo_preview:
                logger.info(
                    "PREVIEW | fecha=%s | activaciones=%s | reactivaciones=%s | total=%s",
                    resultado.get("fecha"),
                    resultado.get("totalActivaciones", 0),
                    resultado.get("totalReactivaciones", 0),
                    resultado.get("totalProcesara", 0),
                )
                for item in resultado.get("detalle", []):
                    finaliza = item.get("finalizara", {})
                    logger.info(
                        "%s | Estudiante %s (%s) | membresía %s → %s ~ %s | finalizará: %s",
                        item.get("tipo"),
                        item.get("idEstudiante"),
                        item.get("nombreEstudiante"),
                        item.get("idMembresiaCore"),
                        item.get("fechaInicio"),
                        item.get("fechaFin"),
                        finaliza.get("idMembresiaCore", "ninguna"),
                    )
            else:
                logger.info(
                    "OK | procesadas=%s | activadas=%s | reactivadas=%s | errores=%s",
                    resultado.get("procesadas", "?"),
                    resultado.get("activadas", "?"),
                    resultado.get("reactivadas", "?"),
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
