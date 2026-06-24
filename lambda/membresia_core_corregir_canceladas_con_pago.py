"""
Lambda: membresia_core_corregir_canceladas_con_pago
====================================================
Ejecución MANUAL, una sola vez.

Corrige dos tipos de inconsistencias:

1. Membresías CANCELADA con pago real asociado (ONLINE/EFECTIVO/TRANSFERENCIA)
   → restaura a FINALIZADA (si vencida) o PAGADA (si aún vigente)

2. Estudiantes cuya membresía activa (esActiva=true) es FINALIZADA pero existe
   una más reciente con pago real
   → activa la más reciente y ajusta estadoPago del estudiante

Parámetros de evento (opcionales):
  preview: true  → solo muestra qué cambiaría, sin guardar (GET /preview)
           false → ejecuta la corrección (POST, valor por defecto)

Variables de entorno requeridas:
  BACKEND_URL      → https://tu-dominio.com  (sin barra final)
  INTERNAL_API_KEY → clave configurada en app.internal.api-key
"""

import json
import os
import urllib.request
import urllib.error

BASE_ENDPOINT = "/api/internal/membresias-core/jobs/corregir-canceladas-con-pago"


def lambda_handler(event, context):
    backend_url = os.environ.get("BACKEND_URL", "").rstrip("/")
    api_key = os.environ.get("INTERNAL_API_KEY", "")

    if not backend_url or not api_key:
        print("ERROR: BACKEND_URL e INTERNAL_API_KEY son requeridos")
        return {"statusCode": 500, "body": "Variables de entorno no configuradas"}

    preview = event.get("preview", False) if isinstance(event, dict) else False

    if preview:
        url = f"{backend_url}{BASE_ENDPOINT}/preview"
        method = "GET"
        data = None
    else:
        url = f"{backend_url}{BASE_ENDPOINT}"
        method = "POST"
        data = b""

    print(f"Modo: {'PREVIEW' if preview else 'EJECUCIÓN'}")
    print(f"Llamando a: {method} {url}")

    req = urllib.request.Request(
        url=url,
        data=data,
        method=method,
        headers={
            "X-Internal-Api-Key": api_key,
            "Content-Type": "application/json",
        },
    )

    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            body = json.loads(response.read().decode())
            print(f"Respuesta {response.status}: {json.dumps(body, ensure_ascii=False)}")

            if preview:
                canceladas = body.get("canceladasConPago_encontradas", 0)
                corregir   = body.get("estudiantesQueSeCorregiran", 0)
                print(
                    f"PREVIEW: {canceladas} canceladas con pago | "
                    f"{corregir} estudiantes con activa incorrecta que se corregirán"
                )
            else:
                restauradas = body.get("canceladasRestauradas", 0)
                corregidos  = body.get("estudiantesCorregidos", 0)
                errores     = body.get("errores", 0)
                print(
                    f"Resultado: {restauradas} canceladas restauradas | "
                    f"{corregidos} estudiantes corregidos | "
                    f"{errores} errores"
                )

            return {"statusCode": 200, "body": body}

    except urllib.error.HTTPError as e:
        error_body = e.read().decode()
        print(f"HTTP {e.code} - {error_body}")
        return {"statusCode": e.code, "body": error_body}

    except Exception as e:
        print(f"Error inesperado: {e}")
        return {"statusCode": 500, "body": str(e)}
