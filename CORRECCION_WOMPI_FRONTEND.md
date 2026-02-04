# Corrección del Widget de Wompi en Frontend

## ✅ Verificado: Las Credenciales Son Válidas

La cuenta de Wompi está correctamente configurada:
- **Comercio**: CLUB DE VOLEIBOL GALACTICOS SM
- **NIT**: 901893028  
- **Estado**: ✅ Activo
- **Public Key**: `pub_test_CSA2EFholZpUOQRXltIiXDQixqVK5Rx1`

## 🔴 Error Actual
```
GET https://api.wompi.co/v1/merchants/undefined 422 (Unprocessable Content)
TypeError: Cannot read properties of undefined (reading 'map')
```

**Causa**: El frontend NO está pasando la `publicKey` correctamente al widget de Wompi. 
La llave llega como `undefined` en lugar del valor correcto.

---

## Instrucciones para Claude (Frontend)

Copia y pega esto en tu chat de Claude para el proyecto frontend:

---

### Prompt para Claude:

```
Necesito que corrijas la integración del widget de Wompi. El backend está en modo sandbox y tiene estos endpoints:

## Endpoints del Backend (localhost:8080)

### 1. Obtener Configuración Pública
GET http://localhost:8080/api/wompi/config

Respuesta:
{
  "publicKey": "pub_test_CSA2EFholZpUOQRXltIiXDQixqVK5Rx1",
  "sandbox": true,
  "currency": "COP"
}

### 2. Generar Firma de Integridad
POST http://localhost:8080/api/wompi/integrity-signature?amount=50000&reference=PAY-123-FEB&currency=COP

Respuesta:
{
  "reference": "PAY-123-FEB",
  "amountInCents": 5000000,
  "currency": "COP",
  "integritySignature": "abc123...",
  "publicKey": "pub_test_..."
}

### 3. Generar Referencia de Pago
GET http://localhost:8080/api/wompi/generate-reference?idEstudiante=1&mesPagado=FEB2026

Respuesta:
{
  "reference": "PAY-1-FEB2026-A1B2C3D4"
}

### 4. Crear Link de Pago (Alternativa)
POST http://localhost:8080/api/wompi/payment-link
Content-Type: application/json

Body:
{
  "idEstudiante": 1,
  "amount": 50000,
  "mesPagado": "FEB2026",
  "customerEmail": "estudiante@email.com",
  "customerName": "Juan Pérez",
  "name": "Mensualidad Febrero",
  "description": "Pago de mensualidad escuela de voleibol",
  "redirectUrl": "http://localhost:3000/pago-resultado"
}

Respuesta exitosa:
{
  "id": "link_id_123",
  "paymentLinkUrl": "https://checkout.wompi.co/l/link_id_123",
  "reference": "PAY-1-FEB2026-A1B2C3D4",
  "amountInCents": 5000000,
  "success": true,
  "message": "Link de pago creado exitosamente"
}

---

## Implementación Correcta del Widget Wompi

### Opción A: Widget Checkout Embebido

El componente de pago debe seguir este flujo:

1. Primero, incluir el script de Wompi en el HTML o cargarlo dinámicamente
2. Obtener la configuración y firma del backend
3. Inicializar el widget con los datos correctos

```jsx
import React, { useState, useEffect } from 'react';

const WompiPayment = ({ estudiante, monto, mesPagado, onPaymentComplete }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Cargar script de Wompi
  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://checkout.wompi.co/widget.js';
    script.async = true;
    document.body.appendChild(script);
    
    return () => {
      document.body.removeChild(script);
    };
  }, []);

  const handlePayment = async () => {
    setLoading(true);
    setError(null);

    try {
      // 1. Generar referencia única
      const refResponse = await fetch(
        `http://localhost:8080/api/wompi/generate-reference?idEstudiante=${estudiante.id}&mesPagado=${mesPagado}`
      );
      const { reference } = await refResponse.json();

      // 2. Obtener firma de integridad
      const signatureResponse = await fetch(
        `http://localhost:8080/api/wompi/integrity-signature?amount=${monto}&reference=${reference}&currency=COP`,
        { method: 'POST' }
      );
      const signatureData = await signatureResponse.json();

      // 3. Abrir widget de Wompi
      const checkout = new window.WidgetCheckout({
        currency: 'COP',
        amountInCents: signatureData.amountInCents,
        reference: signatureData.reference,
        publicKey: signatureData.publicKey,
        signature: {
          integrity: signatureData.integritySignature
        },
        redirectUrl: window.location.origin + '/pago-resultado',
        customerData: {
          email: estudiante.correo || 'estudiante@email.com',
          fullName: `${estudiante.nombre} ${estudiante.apellido}`,
          phoneNumber: estudiante.telefono || '',
          legalId: estudiante.documento || '',
          legalIdType: 'CC'
        }
      });

      checkout.open(function(result) {
        const transaction = result.transaction;
        
        if (transaction && transaction.status === 'APPROVED') {
          onPaymentComplete && onPaymentComplete({
            success: true,
            transactionId: transaction.id,
            reference: reference,
            status: transaction.status
          });
        } else if (transaction) {
          setError(`Pago ${transaction.status}: ${transaction.statusMessage || 'No aprobado'}`);
        }
        
        setLoading(false);
      });

    } catch (err) {
      console.error('Error en pago:', err);
      setError('Error al iniciar el pago. Intente nuevamente.');
      setLoading(false);
    }
  };

  return (
    <div>
      {error && <div className="error-message">{error}</div>}
      <button 
        onClick={handlePayment} 
        disabled={loading}
        className="btn-pagar"
      >
        {loading ? 'Procesando...' : `Pagar $${monto.toLocaleString()} COP`}
      </button>
    </div>
  );
};

export default WompiPayment;
```

### Opción B: Usar Link de Pago (Más Simple)

Si prefieres redirigir al usuario a la página de Wompi:

```jsx
import React, { useState } from 'react';

const WompiPaymentLink = ({ estudiante, monto, mesPagado }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handlePayment = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('http://localhost:8080/api/wompi/payment-link', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          idEstudiante: estudiante.id,
          amount: monto,
          mesPagado: mesPagado,
          customerEmail: estudiante.correo,
          customerName: `${estudiante.nombre} ${estudiante.apellido}`,
          name: `Mensualidad ${mesPagado}`,
          description: 'Pago de mensualidad escuela de voleibol',
          redirectUrl: window.location.origin + '/pago-resultado'
        })
      });

      const data = await response.json();

      if (data.success && data.paymentLinkUrl) {
        // Redirigir a la página de pago de Wompi
        window.location.href = data.paymentLinkUrl;
      } else {
        setError(data.message || 'Error al crear link de pago');
      }

    } catch (err) {
      console.error('Error:', err);
      setError('Error al procesar. Intente nuevamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {error && <div className="error-message">{error}</div>}
      <button 
        onClick={handlePayment} 
        disabled={loading}
        className="btn-pagar"
      >
        {loading ? 'Generando link...' : `Pagar $${monto.toLocaleString()} COP`}
      </button>
    </div>
  );
};

export default WompiPaymentLink;
```

---

## Puntos Importantes a Verificar

1. **amountInCents debe ser número, no string**
   - El backend envía el monto en centavos como número
   - Asegúrate de no convertirlo a string

2. **La referencia debe ser exactamente la misma**
   - La referencia usada para generar la firma debe ser la misma que se envía al widget

3. **El publicKey debe coincidir**
   - En sandbox: `pub_test_CSA2EFholZpUOQRXltIiXDQixqVK5Rx1`

4. **El script de Wompi debe estar cargado**
   - Asegúrate de que `window.WidgetCheckout` existe antes de usarlo

5. **Manejo de CORS**
   - El backend ya tiene CORS habilitado para todos los orígenes

---

## Tarjetas de Prueba (Sandbox)

| Resultado | Número |
|-----------|--------|
| Aprobada | 4242 4242 4242 4242 |
| Declinada | 4111 1111 1111 1111 |

CVV: cualquier 3 dígitos (123)
Fecha: cualquier fecha futura (12/28)
Nombre: cualquier nombre

---

## Página de Resultado de Pago

Crea un componente para manejar el resultado cuando Wompi redirige de vuelta:

```jsx
import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

const PagoResultado = () => {
  const [searchParams] = useSearchParams();
  const [transactionData, setTransactionData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const transactionId = searchParams.get('id');
    
    if (transactionId) {
      // Consultar estado de la transacción
      fetch(`http://localhost:8080/api/wompi/transaction/${transactionId}`)
        .then(res => res.json())
        .then(data => {
          setTransactionData(data);
          setLoading(false);
        })
        .catch(err => {
          console.error(err);
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [searchParams]);

  if (loading) return <div>Verificando pago...</div>;

  if (!transactionData) {
    return <div>No se encontró información de la transacción</div>;
  }

  return (
    <div className="pago-resultado">
      {transactionData.status === 'APPROVED' ? (
        <div className="pago-exitoso">
          <h2>¡Pago Exitoso!</h2>
          <p>Referencia: {transactionData.reference}</p>
          <p>Monto: ${(transactionData.amountInCents / 100).toLocaleString()} COP</p>
        </div>
      ) : (
        <div className="pago-fallido">
          <h2>Pago No Completado</h2>
          <p>Estado: {transactionData.status}</p>
          <p>{transactionData.statusMessage}</p>
        </div>
      )}
    </div>
  );
};

export default PagoResultado;
```

Aplica estos cambios y el error del widget debería resolverse.
```

---

## Resumen de Cambios Necesarios

1. **Cargar correctamente el script de Wompi**
2. **Obtener la firma de integridad del backend ANTES de abrir el widget**
3. **Asegurar que `amountInCents` sea número**
4. **Usar la misma referencia en firma y widget**
5. **Crear página de resultado para manejar redirección**
