// Implementación REAL del servicio de chat contra el backend (IF-01).
// Contrato propuesto: ver README.md › "Contrato de la API REST".
import { request } from './apiClient.js';

export const chatApi = {
  iniciarSesion: (canal) => request('/chat/sesiones', { method: 'POST', body: { canal } }),
  enviarConsulta: (sesionId, texto) =>
    request('/chat/consultas', { method: 'POST', body: { sesionId, texto } }),
  cerrarSesion: (sesionId) => request(`/chat/sesiones/${encodeURIComponent(sesionId)}/cerrar`, { method: 'POST' }),
  obtenerTemas: () => request('/chat/temas'),
};
