"""
Lambda: reconciliar_membresias
==============================
Invocación manual (una sola vez, o cuando se detecte data sucia).

Llama a POST /api/internal/membresias/reconciliar
Revisa cada membresía activa y la compara con los pagos APROBADOS encontrados
en su rango. Si los pagos cubren más meses de los registrados, extiende
la fechaFin y pone al estudiante en AL_DIA.

Caso típico: Wompi registró $150.000 (2 meses) pero la membresía solo muestra 1 mes.

Variables de entorno requeridas:
  BACKEND_URL      → https://tu-dominio.com   (sin barra final)
  INTERNAL_API_KEY → clave configurada en app.internal.api-key
"""

import json
import os
import urllib.request
import urllib.error

ENDPOINT = "/api/internal/membresias/reconciliar"


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

            extendidas = body.get("extendidas", 0)
            sin_cambio = body.get("sinCambio", 0)
            sin_pago   = body.get("sinPago", 0)
            errores    = body.get("errores", 0)
            print(
                f"Resultado: {extendidas} membresías extendidas | "
                f"{sin_cambio} sin cambio | "
                f"{sin_pago} sin pago respaldo | "
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
