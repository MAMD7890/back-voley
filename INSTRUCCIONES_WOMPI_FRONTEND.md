# Instrucciones para Integración de Wompi en Frontend

## Problema Actual

El frontend está intentando inicializar Wompi sin obtener la configuración del backend, lo que causa:
- `GET /merchants/undefined` → Error 422
- `Cannot read properties of undefined (reading 'map')` → TypeError

**Causa raíz:** La `publicKey` es `undefined` porque no se está obteniendo del backend.

---

## Solución

### 1. Crear un Servicio para Wompi

**Archivo:** `src/app/services/wompi.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WompiConfig {
  sandbox: boolean;
  currency: string;
  publicKey: string;
}

@Injectable({
  providedIn: 'root'
})
export class WompiService {

  private apiUrl = 'https://3-85-111-48.nip.io/api/wompi';

  constructor(private http: HttpClient) { }

  /**
   * Obtiene la configuración de Wompi del backend
   * Retorna: { sandbox, currency, publicKey }
   */
  getWompiConfig(): Observable<WompiConfig> {
    return this.http.get<WompiConfig>(`${this.apiUrl}/config`);
  }

  /**
   * Obtiene el resultado de una transacción de pago
   */
  getPaymentResult(reference: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/payment-result/${reference}`);
  }

  /**
   * Calcula la firma de integridad para validar el pago
   */
  calculateIntegritySignature(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/integrity-signature`, data);
  }

  /**
   * Genera una referencia única para la transacción
   */
  generateReference(): Observable<any> {
    return this.http.get(`${this.apiUrl}/generate-reference`);
  }
}
```

---

### 2. Crear un Componente para el Pago

**Archivo:** `src/app/components/pago-wompi/pago-wompi.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { WompiService, WompiConfig } from '../../services/wompi.service';

declare var $wompi: any; // Variable global de Wompi

@Component({
  selector: 'app-pago-wompi',
  templateUrl: './pago-wompi.component.html',
  styleUrls: ['./pago-wompi.component.css']
})
export class PagoWompiComponent implements OnInit {

  wompiConfig: WompiConfig | null = null;
  isLoading = true;
  errorMessage = '';
  sucessMessage = '';

  constructor(private wompiService: WompiService) { }

  ngOnInit(): void {
    this.initializeWompi();
  }

  /**
   * Inicializa Wompi con la configuración del backend
   */
  initializeWompi(): void {
    this.wompiService.getWompiConfig().subscribe({
      next: (config: WompiConfig) => {
        this.wompiConfig = config;
        console.log('Wompi Config recibida:', config);
        
        // Inicializar Wompi con la publicKey
        this.setupWompiWidget(config);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error al obtener configuración de Wompi:', error);
        this.errorMessage = 'Error al cargar la pasarela de pago. Intenta más tarde.';
        this.isLoading = false;
      }
    });
  }

  /**
   * Configura el widget de Wompi
   */
  setupWompiWidget(config: WompiConfig): void {
    if (typeof $wompi === 'undefined') {
      console.error('Script de Wompi no está cargado');
      this.errorMessage = 'Error: Pasarela de pago no disponible';
      return;
    }

    try {
      $wompi.initialize({
        publicKey: config.publicKey,
        currency: config.currency,
        amountInCents: 100000, // 10,000 COP = 100000 centavos
        reference: this.generatePaymentReference(), // Generar referencia única
        customerEmail: 'usuario@example.com', // Obtener del usuario logueado
        customerData: {
          name: 'Nombre del Estudiante',
          phone: '3001234567',
          email: 'usuario@example.com'
        },
        onSuccess: (result: any) => this.onPaymentSuccess(result),
        onError: (error: any) => this.onPaymentError(error),
        onEvent: (event: any) => this.onPaymentEvent(event)
      });
    } catch (error) {
      console.error('Error al inicializar Wompi:', error);
      this.errorMessage = 'Error al inicializar pasarela de pago';
    }
  }

  /**
   * Genera una referencia única para la transacción
   */
  generatePaymentReference(): string {
    const timestamp = Date.now();
    const random = Math.floor(Math.random() * 10000);
    return `PAY-${timestamp}-${random}`;
  }

  /**
   * Maneja el éxito del pago
   */
  onPaymentSuccess(result: any): void {
    console.log('Pago exitoso:', result);
    this.sucessMessage = 'Pago procesado exitosamente';
    
    // Aquí puedes hacer una llamada a tu backend para registrar el pago
    // this.registrarPago(result);
  }

  /**
   * Maneja errores del pago
   */
  onPaymentError(error: any): void {
    console.error('Error en pago:', error);
    
    if (error?.message?.includes('insufficient_funds')) {
      this.errorMessage = 'Saldo insuficiente en la tarjeta';
    } else if (error?.message?.includes('invalid_card')) {
      this.errorMessage = 'Tarjeta inválida';
    } else if (error?.message?.includes('expired_card')) {
      this.errorMessage = 'Tarjeta expirada';
    } else {
      this.errorMessage = error?.message || 'Error al procesar el pago';
    }
  }

  /**
   * Maneja eventos del pago (para debugging)
   */
  onPaymentEvent(event: any): void {
    console.log('Evento de Wompi:', event);
  }
}
```

---

### 3. Crear el Template HTML

**Archivo:** `src/app/components/pago-wompi/pago-wompi.component.html`

```html
<div class="pago-container">
  <h2>Procesar Pago</h2>

  <!-- Indicador de carga -->
  <div *ngIf="isLoading" class="loading">
    <p>Cargando pasarela de pago...</p>
  </div>

  <!-- Mensajes de error -->
  <div *ngIf="errorMessage" class="alert alert-danger">
    {{ errorMessage }}
  </div>

  <!-- Mensaje de éxito -->
  <div *ngIf="sucessMessage" class="alert alert-success">
    {{ sucessMessage }}
  </div>

  <!-- Widget de Wompi -->
  <div *ngIf="wompiConfig && !isLoading" id="wompi-container">
    <!-- El script de Wompi inyectará el widget aquí -->
  </div>

  <!-- Información del pago -->
  <div *ngIf="wompiConfig && !isLoading" class="payment-info">
    <p><strong>Moneda:</strong> {{ wompiConfig.currency }}</p>
    <p><strong>Modo:</strong> {{ wompiConfig.sandbox ? 'Prueba (Sandbox)' : 'Producción' }}</p>
  </div>
</div>
```

---

### 4. Agregar el Script de Wompi en HTML

**Archivo:** `src/index.html` (agregar en `<body>`)

```html
<!-- Wompi Script - se debe cargar ANTES de inicializar -->
<script src="https://checkout.wompi.co/widget.js"></script>
```

O en el componente:

```typescript
loadWompiScript(): void {
  const script = document.createElement('script');
  script.src = 'https://checkout.wompi.co/widget.js';
  script.async = true;
  document.body.appendChild(script);
}
```

---

### 5. Configurar CORS en HttpClient

**Archivo:** `src/app/app.module.ts`

```typescript
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

@NgModule({
  imports: [
    HttpClientModule,
    // ... otros imports
  ]
})
export class AppModule { }
```

---

## Cambios en Endpoints

### Backend está proporcionando:

✅ **GET /api/wompi/config**
```bash
Response: {
  "sandbox": false,
  "currency": "COP",
  "publicKey": "pub_prod_zUER792R9at58I5cxcbi9MdeBUVGN8zZ"
}
```

### El frontend DEBE:

1. ✅ Llamar a `/api/wompi/config` antes de inicializar
2. ✅ Guardar la respuesta en una variable
3. ✅ Pasar `config.publicKey` al widget de Wompi
4. ✅ Manejar errores de tarjeta sin saldo
5. ✅ Manejar errores de tarjeta inválida
6. ✅ Mostrar mensajes claros al usuario

---

## Testeo

### 1. Verificar que el backend devuelve la configuración:

```bash
curl -X GET https://3-85-111-48.nip.io/api/wompi/config
```

**Esperado:**
```json
{
  "sandbox": false,
  "currency": "COP",
  "publicKey": "pub_prod_zUER792R9at58I5cxcbi9MdeBUVGN8zZ"
}
```

### 2. En la consola del navegador, verificar:

```javascript
// Debe estar definida la variable global
console.log($wompi);  // No debe ser undefined

// Debe haber una configuración de Wompi
console.log($wompi.initialize); // Debe ser una función
```

### 3. Verificar que el widget carga:

- Abre DevTools (F12)
- Ve a Network
- Busca peticiones a `api.wompi.co` o `checkout.wompi.co`
- Deberían devolver 200 OK, no 422

---

## Errores Comunes y Soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `Cannot read properties of undefined` | `publicKey` es undefined | Asegúrate de llamar a `/api/wompi/config` ANTES de inicializar |
| `GET /merchants/undefined 422` | publicKey no se pasó a Wompi | Pasa `config.publicKey` al `.initialize()` |
| `CORS error` | Frontend no puede acceder al backend | Verifica que el dominio esté en `SecurityConfig.java` |
| `Insufficient funds` | Tarjeta sin saldo | Muestra mensaje: "Saldo insuficiente" |
| `Script no cargado` | Wompi script no está en el HTML | Agrega `<script src="https://checkout.wompi.co/widget.js"></script>` |

---

## Checklist de Implementación

- [ ] Crear `wompi.service.ts`
- [ ] Crear `pago-wompi.component.ts` y `.html`
- [ ] Agregar script de Wompi en `index.html`
- [ ] Importar HttpClientModule en app.module
- [ ] Inyectar WompiService en el componente
- [ ] Llamar a `getWompiConfig()` en `ngOnInit`
- [ ] Pasar `publicKey` al `$wompi.initialize()`
- [ ] Implementar manejadores de éxito/error
- [ ] Testear en el navegador con DevTools
- [ ] Verificar petición GET a `/api/wompi/config` en Network
- [ ] Probar con una tarjeta de prueba (si está en sandbox)

---

## Contacto / Soporte

Si encuentras errores:

1. Abre la consola (F12)
2. Copia el error completo
3. Verifica el endpoint `/api/wompi/config` con curl
4. Confirma que el dominio está en SecurityConfig.java

**Endpoint del backend para verificar:**
```bash
curl -X GET https://3-85-111-48.nip.io/api/wompi/config \
  -H "Content-Type: application/json"
```

---

## Versiones Recomendadas

- Angular: 14+
- HttpClient: Built-in
- RxJS: 7+
- TypeScript: 4.6+

