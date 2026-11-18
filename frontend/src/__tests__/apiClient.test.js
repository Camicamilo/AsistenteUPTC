import { afterEach, describe, expect, it, vi } from 'vitest';
import { request, TOKEN_KEY } from '../services/apiClient.js';
import { sessionStore } from '../utils/storage.js';

function respuesta(status, cuerpo) {
  return Promise.resolve(new Response(cuerpo == null ? '' : JSON.stringify(cuerpo), { status }));
}

afterEach(() => {
  vi.unstubAllGlobals();
  sessionStore.remove(TOKEN_KEY);
});

describe('cliente de la API REST', () => {
  it('devuelve el JSON cuando la respuesta es exitosa', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respuesta(200, { ok: true })));
    await expect(request('/chat/temas')).resolves.toEqual({ ok: true });
  });

  it('envía el JWT solo en las rutas del panel', async () => {
    sessionStore.set(TOKEN_KEY, 'abc.def.ghi');
    const fetch = vi.fn(() => respuesta(200, {}));
    vi.stubGlobal('fetch', fetch);
    await request('/admin/metricas', { auth: true });
    expect(fetch.mock.calls[0][1].headers.Authorization).toBe('Bearer abc.def.ghi');
    await request('/chat/temas');
    expect(fetch.mock.calls[1][1].headers.Authorization).toBeUndefined();
  });

  it.each([
    [401, 'NO_AUTORIZADO'],
    [403, 'PROHIBIDO'],
    [400, 'VALIDACION'],
    [409, 'CONFLICTO'],
    [503, 'PLN_NO_DISPONIBLE'],
    [500, 'SERVIDOR'],
  ])('traduce el HTTP %i a %s', async (status, codigo) => {
    vi.stubGlobal('fetch', vi.fn(() => respuesta(status, { mensaje: 'x', campo: 'texto' })));
    await expect(request('/x')).rejects.toMatchObject({ codigo });
  });

  it('un 503 del chat trae los datos de contacto de la dependencia', async () => {
    const dependencia = { clave: 'general', nombre: 'Atención al Ciudadano' };
    vi.stubGlobal('fetch', vi.fn(() => respuesta(503, { mensaje: 'caído', dependencia })));
    await expect(request('/chat/consultas', { method: 'POST', body: {} })).rejects.toMatchObject({
      codigo: 'PLN_NO_DISPONIBLE',
      detalles: { dependencia },
    });
  });

  it('un fallo de red se informa como RED', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))));
    await expect(request('/x')).rejects.toMatchObject({ codigo: 'RED' });
  });
});
