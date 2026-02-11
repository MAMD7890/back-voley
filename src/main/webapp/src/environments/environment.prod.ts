/**
 * ENVIRONMENT: PRODUCCIÓN
 * 
 * Configuración para producción en servidor remoto
 * Se usa cuando ejecutas: ng build --prod o ng build --configuration production
 */

export const environment = {
  // ==================== MODO ====================
  production: true,
  development: false,
  
  // ==================== BACKEND API ====================
  // En producción, usa el servidor remoto
  apiUrl: 'https://3-85-111-48.nip.io/api',
  apiAuthUrl: 'https://3-85-111-48.nip.io/api/auth',
  apiFilesUrl: 'https://3-85-111-48.nip.io/api/files',
  
  // ==================== CONFIGURACIÓN DE LOG ====================
  logLevel: 'warn',  // En producción, solo warnings y errores
  enableConsoleLog: false,
  enableRemoteLog: true,
  
  // ==================== CONFIGURACIÓN DE WOMPI ====================
  // IMPORTANTE: sandbox debe ser FALSE en producción
  wompiConfig: {
    sandbox: false,  // ← PRODUCCIÓN: usa claves reales de Wompi
    apiUrl: 'https://3-85-111-48.nip.io/api/wompi',
    enableMockPayments: false
  },
  
  // ==================== CONFIGURACIÓN DE ALMACENAMIENTO ====================
  storage: {
    useLocalStorage: true,
    useSessionStorage: false,
    cacheExpirationMs: 3600000 // 1 hora
  },
  
  // ==================== CONFIGURACIÓN DE AUTENTICACIÓN ====================
  auth: {
    tokenKey: 'access_token',
    refreshTokenKey: 'refresh_token',
    userKey: 'user',
    tokenExpirationTime: 3600000 // 1 hora en ms
  },
  
  // ==================== CONFIGURACIÓN DE APLICACIÓN ====================
  app: {
    name: 'Galácticos Voleibol',
    version: '1.4.0',
    defaultLanguage: 'es',
    supportedLanguages: ['es', 'en'],
    defaultTimeZone: 'America/Bogota'
  }
};
