"""
Lambda: recuperar_pagos_wompi
==============================
Ejecución diaria via EventBridge.
Consulta los últimos 2 días de transacciones de Wompi y registra
los pagos de membresías (referencias PAY-) que no quedaron registrados en la BD.
Solo procesa pagos con referencia PAY-; los demás (uniformes, links externos) son ignorados.

Llama a POST /api/wompi/recuperar-pagos?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
Recorre TODAS las páginas de transacciones de Wompi en el rango indicado.
Por cada transacción APPROVED no registrada, crea el pago y actualiza
la membresía del estudiante (MembresiaCore si app.membresia.usar-core=true).

Variables de entorno requeridas:
  BACKEND_URL      → https://tu-dominio.com   (sin barra final)
  INTERNAL_API_KEY → clave configurada en app.internal.api-key

Variable de entorno opcional:
  DIAS_ATRAS       → días hacia atrás a consultar (default: 2)
"""

import json
import os
import urllib.request
import urllib.error
from datetime import date, timedelta

ENDPOINT = "/api/wompi/recuperar-pagos"


def lambda_handler(event, context):
    backend_url = os.environ.get("BACKEND_URL", "").rstrip("/")
    api_key = os.environ.get("INTERNAL_API_KEY", "")

    if not backend_url or not api_key:
        print("ERROR: BACKEND_URL e INTERNAL_API_KEY son requeridos")
        return {"statusCode": 500, "body": "Variables de entorno no configuradas"}

    dias_atras = int(os.environ.get("DIAS_ATRAS", "2"))
    hasta = date.today()
    desde = hasta - timedelta(days=dias_atras)

    desde_str = desde.isoformat()
    hasta_str = hasta.isoformat()

    url = f"{backend_url}{ENDPOINT}?desde={desde_str}&hasta={hasta_str}"
    print(f"Llamando a: {url}")
    print(f"Rango consultado: {desde_str} → {hasta_str} ({dias_atras} dia(s))")

    req = urllib.request.Request(
        url=url,
        data=b"",
        method="POST",
        headers={
            "X-Internal-Api-Key": api_key,
            "Content-Type": "application/json",
        },
    )

    try:
        # Timeout alto porque pagina todas las transacciones de Wompi
        with urllib.request.urlopen(req, timeout=120) as response:
            body = json.loads(response.read().decode())
            print(f"Respuesta {response.status}: {json.dumps(body, ensure_ascii=False)}")

            total        = body.get("totalConsultadas", 0)
            ya_reg       = body.get("yaRegistradas", 0)
            creados      = body.get("pagosCreados", 0)
            actualizados = body.get("pagosActualizados", 0)
            sin_match    = body.get("sinMatchEstudiante", 0)
            errores      = body.get("errores", 0)

            print(
                f"Resultado: {total} transacciones consultadas | "
                f"{ya_reg} ya registradas | "
                f"{creados} pagos creados | "
                f"{actualizados} pagos actualizados | "
                f"{sin_match} sin match estudiante | "
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
