// Implementación REAL del panel administrativo contra el backend (IF-05, JWT en cada petición).
// Contrato propuesto: ver README.md › "Contrato de la API REST".
import { descargar, request } from './apiClient.js';

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
  historialIntencion: (id) => request(`/admin/intenciones/${id}/historial`, auth),
  dependencias: () => request('/admin/dependencias', auth),

  // Historial de consultas y exportación (UC-15)
  historial: (filtros) => request(`/admin/consultas?${qs(filtros)}`, auth),
  exportarCsv: (rango) => descargar(`/admin/consultas/exportar?${qs({ rango })}`),

  // Cuenta propia
  cambiarContrasena: (actual, nueva) =>
    request('/admin/auth/contrasena', { ...auth, method: 'PUT', body: { actual, nueva } }),

  // Usuarios del panel (UC-14, solo superadministrador)
  listarUsuarios: () => request('/admin/usuarios', auth),
  crearUsuario: (data) => request('/admin/usuarios', { ...auth, method: 'POST', body: data }),
  actualizarUsuario: (id, cambios) => request(`/admin/usuarios/${id}`, { ...auth, method: 'PATCH', body: cambios }),
  restablecerContrasena: (id) => request(`/admin/usuarios/${id}/restablecer`, { ...auth, method: 'POST' }),

  // Configuración del sistema (UC-16) y auditoría
  configuracion: () => request('/admin/configuracion', auth),
  guardarConfiguracion: (data) => request('/admin/configuracion', { ...auth, method: 'PUT', body: data }),
  restaurarConfiguracion: () => request('/admin/configuracion/restaurar', { ...auth, method: 'POST' }),
  actualizarDependencia: (clave, data) =>
    request(`/admin/dependencias/${encodeURIComponent(clave)}`, { ...auth, method: 'PUT', body: data }),
  auditoria: (limite = 100) => request(`/admin/auditoria?${qs({ limite })}`, auth),
  estadoPln: () => request('/admin/pln/estado', auth),
  reentrenarPln: () => request('/admin/pln/reentrenar', { ...auth, method: 'POST' }),
};
