// Configuración central del frontend. Los valores vienen de las variables de entorno de Vite.
const env = import.meta.env;

export const config = {
  useMock: String(env.VITE_USE_MOCK ?? 'true').toLowerCase() !== 'false',
  apiBaseUrl: env.VITE_API_BASE_URL || '/api',
  requestTimeoutMs: Number(env.VITE_REQUEST_TIMEOUT_MS) || 8000,
};

// Reglas de negocio del documento del proyecto.
export const REGLAS = {
  MAX_CARACTERES: 500, // RF-01
  UMBRAL_CONFIANZA_DEFECTO: 0.6, // RF-05
  UMBRAL_MIN: 0.5, // UC-16
  UMBRAL_MAX: 0.8, // UC-16
  INACTIVIDAD_SESION_MIN: 30, // UC-09
  DOMINIO_ADMIN: '@uptc.edu.co', // UC-14
  JWT_HORAS: 8, // Tabla 30
};
