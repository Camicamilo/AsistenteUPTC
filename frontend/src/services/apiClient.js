// Cliente HTTP para la API REST del backend Spring Boot (IF-01 e IF-05).
import { config } from '../config.js';
import { ServiceError } from './errors.js';
import { sessionStore } from '../utils/storage.js';

export const TOKEN_KEY = 'uptc-av-admin-token';

export async function request(path, { method = 'GET', body, auth = false, timeoutMs } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs ?? config.requestTimeoutMs);
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth) {
    const token = sessionStore.get(TOKEN_KEY);
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let res;
  try {
    res = await fetch(`${config.apiBaseUrl}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
  } catch (err) {
    if (err.name === 'AbortError') throw new ServiceError('TIMEOUT', 'Tiempo de espera agotado');
    throw new ServiceError('RED', 'No fue posible conectar con el servidor');
  } finally {
    clearTimeout(timer);
  }

  let data = null;
  const text = await res.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { mensaje: text };
    }
  }

  if (!res.ok) {
    const mensaje = data?.mensaje || data?.message || `Error ${res.status}`;
    if (res.status === 401) throw new ServiceError('NO_AUTORIZADO', mensaje);
    if (res.status === 403) throw new ServiceError('PROHIBIDO', mensaje);
    if (res.status === 400 || res.status === 422) throw new ServiceError('VALIDACION', mensaje, data);
    if (res.status === 409) throw new ServiceError('CONFLICTO', mensaje, data);
    if (res.status === 503) throw new ServiceError('PLN_NO_DISPONIBLE', mensaje, data);
    throw new ServiceError('SERVIDOR', mensaje, data);
  }
  return data;
}
