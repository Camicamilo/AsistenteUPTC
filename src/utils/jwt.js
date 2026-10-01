// Decodifica el payload de un JWT SIN verificar la firma (la verificación la hace el backend).
// Solo se usa en el cliente para saber quién inició sesión y cuándo expira el token.
function b64urlDecode(s) {
  const base = s.replace(/-/g, '+').replace(/_/g, '/');
  const pad = base.length % 4 ? '='.repeat(4 - (base.length % 4)) : '';
  const bin = atob(base + pad);
  const bytes = Uint8Array.from(bin, (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

function b64urlEncode(str) {
  const bytes = new TextEncoder().encode(str);
  let bin = '';
  bytes.forEach((b) => {
    bin += String.fromCharCode(b);
  });
  return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function decodeJwt(token) {
  try {
    const [, payload] = String(token).split('.');
    return JSON.parse(b64urlDecode(payload));
  } catch {
    return null;
  }
}

export function tokenVigente(token) {
  const p = decodeJwt(token);
  return !!p && typeof p.exp === 'number' && p.exp * 1000 > Date.now();
}

// Solo para el modo demostración: genera un token con la misma forma que un JWT real.
export function crearTokenDemo(payload) {
  const header = b64urlEncode(JSON.stringify({ alg: 'none', typ: 'JWT' }));
  const body = b64urlEncode(JSON.stringify(payload));
  return `${header}.${body}.demo`;
}
