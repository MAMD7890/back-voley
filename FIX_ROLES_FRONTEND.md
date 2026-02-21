# 🔧 FIX: Rol ESTUDIANTE no reconocido en Frontend

## 🔍 Problema
El backend ahora devuelve:
```json
{
  "rol": "ESTUDIANTE",
  "usuario": "nombre@email.com"
}
```

Pero el frontend probablemente espera:
```json
{
  "rol": "STUDENT"  
}
```

Por eso ves: `Rol del usuario: ESTUDIANTE` pero se redirige a login.

---

## ✅ Solución en Angular

### 1. En `auth.guard.ts` o similar
**Busca:**
```typescript
if (usuario.rol === 'STUDENT') {
  // permitir acceso
}
```

**Cambia a:**
```typescript
if (usuario.rol === 'ESTUDIANTE') {
  // permitir acceso
}
```

---

### 2. En `auth.service.ts`
**Busca cualquier referencia a 'STUDENT':**
```typescript
// Busca líneas como:
if (this.currentUser.rol === 'STUDENT') { ... }
// O
this.hasRole('STUDENT') { ... }
```

**Cambia TODAS a 'ESTUDIANTE':**
```typescript
if (this.currentUser.rol === 'ESTUDIANTE') { ... }
this.hasRole('ESTUDIANTE') { ... }
```

---

### 3. En componentes de routing/guards
**Busca en:**
- `app.routes.ts` o `app-routing.module.ts`
- `can-activate.guard.ts`
- `role.guard.ts`
- `student.guard.ts`

**Reemplaza:**
```typescript
// ANTES
caso 'STUDENT': { redirigir a: '/estudiante/dashboard' }

// DESPUÉS  
caso 'ESTUDIANTE': { redirigir a: '/estudiante/dashboard' }
```

---

### 4. En plantillas HTML
**Busca:**
```html
*ngIf="usuario?.rol === 'STUDENT'"
*ngIf="!isStudent('STUDENT')"
```

**Cambia a:**
```html
*ngIf="usuario?.rol === 'ESTUDIANTE'"
*ngIf="!isStudent('ESTUDIANTE')"
```

---

## 🔍 Comando para encontrar todas las referencias

```bash
# En Windows PowerShell
cd tu-proyecto-angular
Select-String -Path "src/**/*.ts" -Pattern "STUDENT" -Recurse

# En Mac/Linux
grep -r "STUDENT" src/ --include="*.ts"
```

---

## 📋 Checklist de cambios

- [ ] Auth Guard: cambiar STUDENT → ESTUDIANTE
- [ ] Auth Service: cambiar STUDENT → ESTUDIANTE  
- [ ] Routing module: cambiar STUDENT → ESTUDIANTE
- [ ] Componentes: cambiar STUDENT → ESTUDIANTE
- [ ] Plantillas HTML: cambiar STUDENT → ESTUDIANTE
- [ ] Constantes globales: cambiar STUDENT → ESTUDIANTE
- [ ] Tests: cambiar STUDENT → ESTUDIANTE

---

## ✨ Después del cambio
El flujo funcionará así:

1. ✅ Usuario inicia sesión
2. ✅ Backend devuelve: `"rol": "ESTUDIANTE"`
3. ✅ Frontend reconoce el rol ESTUDIANTE
4. ✅ Guard permite acceso (no redirige a login)
5. ✅ Redirige a dashboard correcto

---

## 🔗 Archivos relacionados

**Backend:**
- [DataInitializer.java](src/main/java/galacticos_app_back/galacticos/config/DataInitializer.java)
- [AuthService.java](src/main/java/galacticos_app_back/galacticos/service/AuthService.java)

**Frontend (buscar en tu proyecto Angular):**
- `src/app/guards/auth.guard.ts`
- `src/app/guards/role.guard.ts`
- `src/app/services/auth.service.ts`
- `src/app/app-routing.module.ts`

---

## ❓ Referencia de roles

| Backend | Frontend | Uso |
|---------|----------|-----|
| USER | USER | Usuario genérico |
| ESTUDIANTE | ESTUDIANTE | Estudiante (membresía) |
| PROFESOR | PROFESOR | Profesor/Entrenador |

