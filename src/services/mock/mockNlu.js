// Motor PLN SIMULADO. Imita la salida de Rasa/Dialogflow (intención + entidades + confianza)
// con un clasificador por palabras clave tolerante a tildes, plurales y errores de tipeo.
// En producción esta lógica vive en el motor PLN; el frontend solo recibe el resultado.
import { normalizar, tokenizar } from '../../utils/text.js';

const STOPWORDS = new Set(
  'a al algo como con de del el ella en es esa ese eso esta este la las le lo los me mi mis muy no o para pero por que se si sin su sus te tu un una uno y ya yo son hay tengo quiero necesito puedo saber cual donde favor porfa porfavor ustedes usted'.split(
    ' ',
  ),
);

function raiz(palabra) {
  return palabra.length > 5 ? palabra.slice(0, palabra.length - 2) : palabra;
}

function distancia(a, b) {
  if (Math.abs(a.length - b.length) > 1) return 2;
  const dp = Array.from({ length: a.length + 1 }, (_, i) => [i]);
  for (let j = 1; j <= b.length; j += 1) dp[0][j] = j;
  for (let i = 1; i <= a.length; i += 1) {
    for (let j = 1; j <= b.length; j += 1) {
      dp[i][j] = Math.min(
        dp[i - 1][j] + 1,
        dp[i][j - 1] + 1,
        dp[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1),
      );
    }
  }
  return dp[a.length][b.length];
}

function coincide(token, clave) {
  if (token === clave) return 1;
  const r = raiz(clave);
  if (r.length >= 4 && token.startsWith(r)) return 0.9;
  if (clave.length >= 6 && token.length >= 6 && distancia(token, clave) <= 1) return 0.8;
  return 0;
}

function puntuar(tokens, intencion) {
  let puntos = 0;
  const usadas = new Set();
  tokens.forEach((t) => {
    let mejor = 0;
    let claveMejor = null;
    Object.entries(intencion.claves || {}).forEach(([clave, peso]) => {
      if (usadas.has(clave)) return;
      const c = coincide(t, clave) * peso;
      if (c > mejor) {
        mejor = c;
        claveMejor = clave;
      }
    });
    if (claveMejor) {
      usadas.add(claveMejor);
      puntos += mejor;
    }
  });
  return puntos;
}

function extraerEntidades(texto) {
  const n = normalizar(texto);
  const entidades = [];
  const periodo = n.match(/\b(20\d{2})\s*[-–]?\s*(i{1,2}|1|2)\b/);
  if (periodo) entidades.push({ tipo: 'periodo', valor: `${periodo[1]}-${periodo[2].length > 1 || periodo[2] === '2' ? 'II' : 'I'}` });
  const tramites = ['matricula', 'certificado', 'carnet', 'reintegro', 'cancelacion', 'inscripcion'];
  tramites.forEach((t) => {
    if (n.includes(t)) entidades.push({ tipo: 'tramite', valor: t });
  });
  if (/proximo semestre|siguiente semestre/.test(n)) entidades.push({ tipo: 'fecha', valor: 'próximo semestre' });
  return entidades;
}

/**
 * Clasifica una consulta.
 * @returns {{ intent: object|null, confianza: number, entidades: object[] }}
 */
export function clasificar(texto, intenciones) {
  const tokens = tokenizar(texto).filter((t) => !STOPWORDS.has(t));
  const puntajes = intenciones
    .map((it) => ({ it, puntos: puntuar(tokens, it) }))
    .filter((p) => p.puntos > 0)
    .sort((a, b) => b.puntos - a.puntos);

  // Las intenciones sociales solo ganan si nada más coincide.
  const tematicas = puntajes.filter((p) => !p.it.social && p.puntos >= 1.5);
  const ranking = tematicas.length ? tematicas : puntajes;

  if (!ranking.length) {
    return { intent: null, confianza: 0.18, entidades: extraerEntidades(texto) };
  }

  const [primero, segundo] = ranking;
  let confianza = 1 - Math.exp(-primero.puntos / 2.2);
  if (segundo && (primero.puntos - segundo.puntos) / primero.puntos < 0.25) confianza *= 0.85;
  confianza = Math.max(0.05, Math.min(0.97, confianza));

  return {
    intent: primero.it,
    confianza: Math.round(confianza * 100) / 100,
    entidades: extraerEntidades(texto),
  };
}
