import { describe, expect, it } from 'vitest';
import { normalizar, tokenizar } from '../utils/text.js';
import { crearTokenDemo, decodeJwt, tokenVigente } from '../utils/jwt.js';
import { clasificar } from '../services/mock/mockNlu.js';
import { INTENCIONES_SEED } from '../services/mock/seedData.js';

describe('texto', () => {
  it('normaliza tildes, mayúsculas y signos', () => {
    expect(normalizar('¿Cuándo son las MATRÍCULAS?')).toBe('cuando son las matriculas');
  });
  it('tokeniza sin vacíos', () => {
    expect(tokenizar('  hola,   mundo ')).toEqual(['hola', 'mundo']);
  });
});

describe('jwt', () => {
  it('decodifica el payload y valida la expiración', () => {
    const exp = Math.floor(Date.now() / 1000) + 60;
    const token = crearTokenDemo({ sub: 1, correo: 'admin@uptc.edu.co', exp });
    expect(decodeJwt(token).correo).toBe('admin@uptc.edu.co');
    expect(tokenVigente(token)).toBe(true);
    expect(tokenVigente(crearTokenDemo({ exp: 1 }))).toBe(false);
    expect(decodeJwt('basura')).toBeNull();
  });
});

describe('motor PLN simulado (modo demostración)', () => {
  const intenciones = INTENCIONES_SEED.map((it, i) => ({ ...it, id: i + 1 }));
  it('clasifica una pregunta frecuente', () => {
    const r = clasificar('¿Cuándo son las matrículas?', intenciones);
    expect(r.intent.nombre).toBe('consulta_matricula');
    expect(r.confianza).toBeGreaterThanOrEqual(0.6);
  });
  it('sin coincidencias devuelve confianza baja', () => {
    const r = clasificar('xyzzy qwerty', intenciones);
    expect(r.intent).toBeNull();
    expect(r.confianza).toBeLessThan(0.6);
  });
});
