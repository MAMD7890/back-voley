"""
Lambda: corregir_pagos_externos
================================
Invocación manual (una sola vez, o cuando se detecte data sucia).

Busca pagos PAGADOS cuya referencia no empieza con "PAY-" (links externos
de Wompi como uniformes, matrículas, etc.) que se registraron incorrectamente
en la BD como si fueran pagos de membresía.

Para cada uno:
  1. Recalcula la fechaFin de la membresía usando solo pagos PAY- reales.
  2. Corrige el estado del estudiante si la membresía recalculada ya venció.
  3. Elimina el pago externo de la BD.

Variables de entorno requeridas:
  BACKEND_URL      → https://tu-dominio.com   (sin barra final)
  INTERNAL_API_KEY → clave configurada en app.internal.api-key
"""

import json
import os
import urllib.request
import urllib.error

ENDPOINT = "/api/internal/membresias/corregir-pagos-externos"


def lambda_handler(event, context):
    backend_url = os.environ.get("BACKEND_URL", "").rstrip("/")
    api_key = os.environ.get("INTERNAL_API_KEY", "")

    if not backend_url or not api_key:
        print("ERROR: BACKEND_URL e INTERNAL_API_KEY son requeridos")
        return {"statusCode": 500, "body": "Variables de entorno no configuradas"}

    url = backend_url + ENDPOINT
    print(f"Llamando a: {url}")

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
        with urllib.request.urlopen(req, timeout=60) as response:
            body = json.loads(response.read().decode())
            print(f"Respuesta {response.status}: {json.dumps(body, ensure_ascii=False)}")

            encontrados       = body.get("pagosExternosEncontrados", 0)
            eliminados        = body.get("pagosEliminados", 0)
            mem_corregidas    = body.get("membresiasCorregidas", 0)
            sin_cambio        = body.get("sinCambio", 0)
            errores           = body.get("errores", 0)

            print(
                f"Resultado: {encontrados} pagos externos encontrados | "
                f"{eliminados} eliminados | "
                f"{mem_corregidas} membresías corregidas | "
                f"{sin_cambio} sin cambio | "
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
