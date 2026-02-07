# 💳 INTEGRACIÓN WOMPI - GUÍA PARA FRONTEND

## 📋 Tabla de Contenidos

1. [Obtener Credenciales](#obtener-credenciales)
2. [Estructura de Pago](#estructura-de-pago)
3. [Implementación Frontend](#implementación-frontend)
4. [Ejemplos de Código](#ejemplos-de-código)
5. [Validaciones](#validaciones)
6. [Manejo de Errores](#manejo-de-errores)

---

## 🔑 Obtener Credenciales

### Paso 1: Crear Cuenta en Wompi
1. Ve a: https://wompi.co
2. Crea una cuenta comercial
3. Verifica tu email y datos

### Paso 2: Obtener API Keys
1. Dashboard → Settings → API Keys
2. Verifica que estés en "Production"
3. Copia:
   - **Public Key** (úsalo en frontend)
   - **Private Key** (úsalo en backend ✅ ya está)
   - **Integrity Secret** (para firma)
   - **Events Secret** (para webhooks)

### Paso 3: Configurar Webhooks
1. Dashboard → Webhooks
2. URL: `https://3.85.111.48:8080/api/wompi/webhook` (o tu dominio)
3. Eventos: selecciona `transaction.updated`
4. Copia el "Events Secret"

---

## 💰 Estructura de Pago

```
Usuario (Frontend)
    ↓
    ├─ Solicita crear pago
    ↓
Backend
    ├─ Crea pago en BD (estado: PENDIENTE)
    ├─ Retorna referencia única
    ↓
Frontend
    ├─ Obtiene firma de integridad
    ├─ Abre Widget de Wompi
    ├─ Usuario ingresa datos de tarjeta
    ↓
Wompi
    ├─ Procesa el pago
    ├─ Envía webhook a Backend
    ↓
Backend
    ├─ Valida webhook
    ├─ Actualiza estado en BD (AL_DIA)
    ├─ Envía confirmación por email
    ↓
Frontend
    ├─ Usuario redirigido a /success
    ├─ Página muestra "Pago confirmado"
```

---

## 📝 Implementación Frontend

### Opción 1: JavaScript Vanilla

```html
<!DOCTYPE html>
<html>
<head>
    <script src="https://checkout.wompi.co/widget.js"></script>
</head>
<body>
    <div id="checkout-container"></div>

    <script>
        // 1. Obtener firma de integridad
        async function getIntegritySignature(amount, reference) {
            const response = await fetch(
                `http://3.85.111.48:8080/api/wompi/integrity-signature?` +
                `amount=${amount}&reference=${reference}&currency=COP`,
                {
                    headers: {
                        'Content-Type': 'application/json'
                    }
                }
            );
            
            return await response.json();
        }

        // 2. Inicializar widget
        async function initializeCheckout() {
            const amount = 50000; // 500 COP = 50000 centavos
            const reference = 'PAG-' + Date.now();
            
            const { integritySignature, publicKey } = await getIntegritySignature(
                amount,
                reference
            );

            const checkout = new WidgetCheckout({
                currency: 'COP',
                amountInCents: amount,
                reference: reference,
                publicKey: publicKey,
                integritySignature: integritySignature,
                redirectUrl: 'https://d2ga9msb3312dv.cloudfront.net/pago-exitoso',
                customerEmail: 'usuario@example.com',
                customerName: 'Juan Pérez'
            });

            checkout.render('#checkout-container');
        }

        // Inicializar cuando la página carga
        document.addEventListener('DOMContentLoaded', initializeCheckout);
    </script>
</body>
</html>
```

### Opción 2: Angular

```typescript
// payment.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = 'http://3.85.111.48:8080/api/wompi';

  constructor(private http: HttpClient) {}

  getIntegritySignature(amount: number, reference: string): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/integrity-signature?amount=${amount}&reference=${reference}&currency=COP`
    );
  }

  createPaymentLink(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/create-payment-link`, data);
  }

  getTransactionStatus(transactionId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/transaction/${transactionId}`);
  }
}

// payment.component.ts
import { Component, OnInit } from '@angular/core';
import { PaymentService } from './payment.service';

declare var WidgetCheckout: any;

@Component({
  selector: 'app-payment',
  templateUrl: './payment.component.html'
})
export class PaymentComponent implements OnInit {
  amount: number = 50000; // 500 COP en centavos

  constructor(private paymentService: PaymentService) {}

  ngOnInit() {
    this.initializeCheckout();
  }

  initializeCheckout() {
    const reference = 'PAG-' + Date.now();

    this.paymentService.getIntegritySignature(this.amount, reference)
      .subscribe((data: any) => {
        const checkout = new WidgetCheckout({
          currency: 'COP',
          amountInCents: this.amount,
          reference: reference,
          publicKey: data.publicKey,
          integritySignature: data.integritySignature,
          redirectUrl: 'https://d2ga9msb3312dv.cloudfront.net/pago-exitoso',
          customerEmail: 'usuario@example.com'
        });

        checkout.render('#checkout-container');
      });
  }
}
```

### Opción 3: React

```jsx
// usePayment.js
import { useEffect, useState } from 'react';

export function usePayment() {
  const [integrityData, setIntegrityData] = useState(null);

  async function getIntegritySignature(amount, reference) {
    const response = await fetch(
      `http://3.85.111.48:8080/api/wompi/integrity-signature?` +
      `amount=${amount}&reference=${reference}&currency=COP`
    );
    return await response.json();
  }

  useEffect(() => {
    const initPayment = async () => {
      const amount = 50000;
      const reference = 'PAG-' + Date.now();
      
      const data = await getIntegritySignature(amount, reference);
      setIntegrityData(data);

      // Cargar widget de Wompi
      if (!window.WidgetCheckout) {
        const script = document.createElement('script');
        script.src = 'https://checkout.wompi.co/widget.js';
        document.body.appendChild(script);

        script.onload = () => {
          const checkout = new window.WidgetCheckout({
            currency: 'COP',
            amountInCents: amount,
            reference: reference,
            publicKey: data.publicKey,
            integritySignature: data.integritySignature,
            redirectUrl: 'https://d2ga9msb3312dv.cloudfront.net/pago-exitoso'
          });

          checkout.render('#checkout-container');
        };
      }
    };

    initPayment();
  }, []);

  return integrityData;
}

// PaymentPage.jsx
import { usePayment } from './usePayment';

export function PaymentPage() {
  const paymentData = usePayment();

  return (
    <div>
      <h1>Realizar Pago</h1>
      {paymentData ? (
        <div id="checkout-container"></div>
      ) : (
        <p>Cargando...</p>
      )}
    </div>
  );
}
```

---

## 📌 Ejemplos de Código

### Crear Pago Completo

```javascript
// Crear estudiante con pago
async function crearEstudianteConPago(estudiante) {
  // 1. Crear estudiante primero
  const studResponse = await fetch('http://3.85.111.48:8080/api/estudiantes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(estudiante)
  });

  const student = await studResponse.json();
  
  // 2. Crear pago
  const paymentData = {
    description: `Cuota de membresía - ${student.nombreCompleto}`,
    amountInCents: 5000000, // 50,000 COP
    customerEmail: student.correoEstudiante,
    customerName: student.nombreCompleto,
    reference: `EST-${student.idEstudiante}-${Date.now()}`,
    redirectUrl: 'https://d2ga9msb3312dv.cloudfront.net/pago-exitoso'
  };

  const payResponse = await fetch('http://3.85.111.48:8080/api/wompi/create-payment-link', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(paymentData)
  });

  const payment = await payResponse.json();
  
  // 3. Redirigir a Wompi
  if (payment.success) {
    window.location.href = payment.paymentLinkUrl;
  }
}
```

### Verificar Estado de Pago

```javascript
async function verificarEstadoPago(transactionId) {
  const response = await fetch(
    `http://3.85.111.48:8080/api/wompi/transaction/${transactionId}`
  );

  const transaction = await response.json();

  switch (transaction.status) {
    case 'APPROVED':
      console.log('✅ Pago aprobado');
      mostrarMensaje('¡Pago realizado exitosamente!');
      break;
    case 'DECLINED':
      console.log('❌ Pago rechazado');
      mostrarMensaje('El pago fue rechazado. Intenta de nuevo.');
      break;
    case 'PENDING':
      console.log('⏳ Pago pendiente');
      mostrarMensaje('El pago está en proceso...');
      break;
    default:
      console.log('❓ Estado desconocido');
  }
}
```

---

## ✅ Validaciones

### Validación de Monto

```javascript
function validarMonto(monto) {
  // Monto mínimo en Wompi: 1000 COP
  if (monto < 100000) {
    throw new Error('El monto mínimo es 1,000 COP');
  }

  // Monto máximo: 999,999,999 COP
  if (monto > 99999999900) {
    throw new Error('El monto máximo es 999,999,999 COP');
  }

  return true;
}
```

### Validación de Email

```javascript
function validarEmail(email) {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
}
```

### Validación de Referencia

```javascript
function validarReferencia(reference) {
  // La referencia debe ser única y contener letras, números y guiones
  if (!/^[a-zA-Z0-9-]+$/.test(reference)) {
    throw new Error('La referencia solo puede contener letras, números y guiones');
  }

  if (reference.length > 140) {
    throw new Error('La referencia no puede exceder 140 caracteres');
  }

  return true;
}
```

---

## 🚨 Manejo de Errores

### Errores Comunes

```javascript
async function crearPago(data) {
  try {
    // Validar datos
    if (!data.customerEmail) {
      throw new Error('Email del cliente es requerido');
    }

    if (!validarEmail(data.customerEmail)) {
      throw new Error('Email inválido');
    }

    if (!validarMonto(data.amountInCents)) {
      throw new Error('Monto inválido');
    }

    // Crear pago
    const response = await fetch('http://3.85.111.48:8080/api/wompi/create-payment-link', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Error al crear pago');
    }

    const payment = await response.json();

    if (!payment.success) {
      throw new Error(payment.message || 'El pago no pudo crearse');
    }

    return payment;

  } catch (error) {
    console.error('Error en pago:', error.message);
    mostrarError(error.message);
    throw error;
  }
}
```

### Reintentos Automáticos

```javascript
async function crearPagoConReintentos(data, maxReintentos = 3) {
  for (let intento = 0; intento < maxReintentos; intento++) {
    try {
      return await crearPago(data);
    } catch (error) {
      if (intento === maxReintentos - 1) {
        throw error;
      }
      
      // Esperar antes de reintentar (exponencial backoff)
      const delay = Math.pow(2, intento) * 1000;
      console.log(`Reintentando en ${delay}ms...`);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
}
```

---

## 🔄 Flujo de Redirección

Después de que Wompi procesa el pago:

```javascript
// En la página de redirección (ej: /pago-exitoso)
function verificarPagoEnRedireccion() {
  const params = new URLSearchParams(window.location.search);
  const reference = params.get('reference');
  const status = params.get('status');

  if (status === 'APPROVED') {
    // Pago aprobado
    console.log('✅ Pago aprobado para referencia:', reference);
    mostrarExito('¡Tu pago fue procesado exitosamente!');
    
    // Opcionalmente, consultar BD para confirmar
    verificarPagoEnServidor(reference);
  } else if (status === 'DECLINED') {
    // Pago rechazado
    console.log('❌ Pago rechazado');
    mostrarError('El pago fue rechazado. Intenta con otra tarjeta.');
  }
}

function verificarPagoEnServidor(reference) {
  // Llamar endpoint para verificar
  fetch(`http://3.85.111.48:8080/api/wompi/transaction/${reference}`)
    .then(r => r.json())
    .then(data => {
      if (data.status === 'APPROVED') {
        // Actualizar UI
        console.log('✅ Pago confirmado en servidor');
      }
    });
}
```

---

## 📊 Códigos de Estado

| Estado | Significado | Acción |
|--------|-------------|--------|
| `APPROVED` | Pago aprobado | ✅ Procesar |
| `DECLINED` | Pago rechazado | ❌ Mostrar error |
| `PENDING` | En proceso | ⏳ Esperar webhook |
| `VOIDED` | Anulado | ❌ No procesar |
| `REFUNDED` | Reembolsado | 🔄 Revertir |

---

## 📚 Recursos Adicionales

- Widget Documentation: https://docs.wompi.co/widget
- API Reference: https://docs.wompi.co/api
- Testing: https://docs.wompi.co/testing
- Dashboard: https://dashboard.wompi.co

---

¡Tu integración está completa! 🎉

