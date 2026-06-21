"""
Lambda: galacticos-membresia-recuperar-faltantes
Trigger: MANUAL — ejecutar DESPUÉS de validar con el preview.

Fase 1 — Pagos huérfanos: pagos ONLINE PAGADOS sin membresía asociada → crea la membresía.
Fase 2 — Duraciones incorrectas: membresías ONLINE cuya duración no coincide con el plan
          (precio_matricula → duracion_meses) → corrige fechaInicio/fechaFin en cadena.

Es idempotente: si la membresía ya existe la omite; si las fechas ya son correctas no toca nada.

Modos de ejecución:
  {"preview": true}   → solo consulta, sin cambios (muestra qué se crearía/corregiría)
  {}                  → ejecuta ambas fases

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
                    "PREVIEW | huerfanos=%s | seCrearan=%s | duracionesIncorrectas=%s",
                    resultado.get("pagosHuerfanos", 0),
                    resultado.get("seCrearan", 0),
                    resultado.get("duracionesIncorrectas", 0),
                )
                for item in resultado.get("detalle_crear", []):
                    nueva = item.get("membresiaQueQuedara", {})
                    logger.info(
                        "CREAR | Estudiante %s (%s) | %s -> %s (%s mes(es), %s) | estadoQuedara=%s",
                        item.get("idEstudiante"),
                        item.get("nombreEstudiante"),
                        nueva.get("fechaInicio"),
                        nueva.get("fechaFin"),
                        nueva.get("meses"),
                        nueva.get("estado"),
                        item.get("estadoQueQuedara"),
                    )
                for est in resultado.get("detalle_corregir", []):
                    for cambio in est.get("cambios", []):
                        logger.info(
                            "CORREGIR | Estudiante %s (%s) | mc=%s | %s->%s (actual) => %s->%s (correcto) | %s mes(es)",
                            est.get("idEstudiante"),
                            est.get("nombreEstudiante"),
                            cambio.get("idMembresiaCore"),
                            cambio.get("fechaInicioActual"),
                            cambio.get("fechaFinActual"),
                            cambio.get("fechaInicioCorregida"),
                            cambio.get("fechaFinCorregida"),
                            cambio.get("mesesEsperados"),
                        )
                    logger.info(
                        "CORREGIR | Estudiante %s estadoQuedara=%s",
                        est.get("idEstudiante"),
                        est.get("estadoQueQuedara"),
                    )
            else:
                logger.info(
                    "OK | huerfanos=%s creadas=%s omitidas=%s errores=%s | duracionesCorregidas=%s erroresCorreccion=%s",
                    resultado.get("pagosHuerfanos", "?"),
                    resultado.get("creadas", "?"),
                    resultado.get("omitidas", "?"),
                    resultado.get("errores", "?"),
                    resultado.get("duracionesCorregidas", "?"),
                    resultado.get("erroresCorreccion", "?"),
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
