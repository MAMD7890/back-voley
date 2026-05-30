"""
Lambda: galacticos-membresia-corregir-fechas-diapago
Trigger: MANUAL — ejecutar DESPUÉS de validar con el preview.

Detecta y corrige membresías cuya fechaFin fue calculada usando diaPago
como billing anchor en lugar de fechaInicio + N meses exactos.

Por cada membresía con error:
  - Recalcula fechaFin como fechaInicio + N meses (según valor del pago)
  - Actualiza estadoMembresia (PAGADA si aún vigente, FINALIZADA si venció)
  - Si esActiva=true, actualiza estadoPago del estudiante

Invocar con {"preview": true} para solo ver los errores sin aplicar cambios.

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
        endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/corregir-fechas-diapago/preview"
        method = "GET"
        data = None
    else:
        endpoint = f"{BACKEND_URL}/api/internal/membresias-core/jobs/corregir-fechas-diapago"
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
                    "Evaluadas=%s | ConError=%s | Correctas=%s",
                    resultado.get("totalEvaluadas", 0),
                    resultado.get("conErrorFecha", 0),
                    resultado.get("correctas", 0),
                )
                for item in resultado.get("detalle_errores", []):
                    logger.info(
                        "ERROR | Membresia %s | Estudiante %s (%s) | esActiva=%s | "
                        "fechaFin actual=%s → correcta=%s (%+d dias) | "
                        "estadoQueQuedara=%s | estadoPagoQueQuedara=%s",
                        item.get("idMembresiaCore"),
                        item.get("idEstudiante"),
                        item.get("nombreEstudiante"),
                        item.get("esActiva"),
                        item.get("fechaFin_actual"),
                        item.get("fechaFin_correcta"),
                        item.get("diferenciaDias", 0),
                        item.get("estadoQueQuedara"),
                        item.get("estadoPagoQueQuedara"),
                    )
            else:
                logger.info(
                    "evaluadas=%s corregidas=%s sinCambio=%s errores=%s",
                    resultado.get("evaluadas", "?"),
                    resultado.get("corregidas", "?"),
                    resultado.get("sinCambio", "?"),
                    resultado.get("errores", "?"),
                )

            return {"statusCode": 200, "body": resultado}

    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        logger.error("HTTP %d: %s", e.code, body)
        raise RuntimeError(f"Backend respondio {e.code}: {body}")

    except urllib.error.URLError as e:
        logger.error("Conexion fallida: %s", str(e.reason))
        raise RuntimeError(f"Error de conexion: {e.reason}")

    except Exception as e:
        logger.error("Error inesperado: %s", str(e))
        raise
