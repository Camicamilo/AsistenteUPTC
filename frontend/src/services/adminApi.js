// Implementación REAL del panel administrativo contra el backend (IF-05, JWT en cada petición).
// Contrato propuesto: ver README.md › "Contrato de la API REST".
import { request } from './apiClient.js';

const qs = (params) =>
  new URLSearchParams(Object.entries(params || {}).filter(([, v]) => v !== '' && v != null)).toString();

const auth = { auth: true };

export const adminApi = {
  MAX_RESPUESTA: 1500,
  login: (correo, password) => request('/admin/auth/login', { method: 'POST', body: { correo, password } }),

  metricas: (rango) => request(`/admin/metricas?${qs({ rango })}`, auth),

  listarIntenciones: (filtros) => request(`/admin/intenciones?${qs(filtros)}`, auth),
  obtenerIntencion: (id) => request(`/admin/intenciones/${id}`, auth),
  crearIntencion: (data) => request('/admin/intenciones', { ...auth, method: 'POST', body: data }),
  actualizarIntencion: (id, data) => request(`/admin/intenciones/${id}`, { ...auth, method: 'PUT', body: data }),
  crearRespuesta: (intentId, texto) =>
    request(`/admin/intenciones/${intentId}/respuestas`, { ...auth, method: 'POST', body: { texto } }),
  actualizarRespuesta: (intentId, respuestaId, cambios) =>
    request(`/admin/intenciones/${intentId}/respuestas/${respuestaId}`, { ...auth, method: 'PATCH', body: cambios }),
};
