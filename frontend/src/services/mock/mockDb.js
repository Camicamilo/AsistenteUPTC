// "Base de datos" en memoria para el modo demostración. Replica las tablas del modelo
// entidad-relación (usuarios, sesiones_chat, consultas, intenciones, respuestas) y se
// guarda en localStorage para que el chat y el panel compartan los mismos datos.
import { storage } from '../../utils/storage.js';
import {
  CONFIG_DEFECTO,
  CONSULTAS_FUERA_DOMINIO,
  INTENCIONES_SEED,
  USUARIOS_SEED,
} from './seedData.js';

const KEY = 'uptc-av-mock-db';
const VERSION = 3;
const DIA = 24 * 60 * 60 * 1000;

let db = null;

function rngSemilla(seed) {
  let a = seed;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Distribución de consultas por hora: picos en la mañana y la tarde, y uso nocturno
// de estudiantes que trabajan (justificación del proyecto).
const PESO_HORA = [1, 0.5, 0.3, 0.2, 0.2, 0.4, 1.2, 3, 5.5, 6.5, 6, 5, 3.5, 3, 4.5, 5.5, 5, 4, 3, 3.2, 3.6, 3, 2, 1.3];
// Popularidad relativa de cada intención en el historial de ejemplo.
const PESO_INTENT = {
  consulta_matricula: 9,
  pasos_matricula: 6,
  correo_institucional_activar: 6,
  info_admisiones: 5,
  calendario_academico: 5,
  correo_recuperar_contrasena: 4,
  certificado_estudio: 3.5,
  acceso_plataformas: 3.5,
  carnet_estudiantil: 3,
  horarios_clases: 3,
  requisitos_admision: 2.5,
  resultados_admision: 2,
  orientacion_primer_semestre: 2,
  cancelacion_asignaturas: 2,
  bienestar_universitario: 1.5,
  reintegro_aplazamiento: 1,
  biblioteca: 1,
  saludo: 2,
  agradecimiento: 1.5,
  despedida: 0.8,
};

function elegirPonderado(rng, items, pesos) {
  const total = pesos.reduce((s, p) => s + p, 0);
  let r = rng() * total;
  for (let i = 0; i < items.length; i += 1) {
    r -= pesos[i];
    if (r <= 0) return items[i];
  }
  return items[items.length - 1];
}

function generarHistorial(intenciones) {
  const rng = rngSemilla(2026);
  const ahora = new Date();
  const hoy0 = new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate()).getTime();
  const horas = Array.from({ length: 24 }, (_, h) => h);
  const pesosIntent = intenciones.map((it) => PESO_INTENT[it.nombre] ?? 1);
  const consultas = [];
  let id = 1;
  let sesion = 1;

  for (let d = 59; d >= 0; d -= 1) {
    const inicioDia = hoy0 - d * DIA;
    const diaSemana = new Date(inicioDia).getDay();
    const finDeSemana = diaSemana === 0 || diaSemana === 6;
    const enMatriculas = d >= 8 && d <= 14; // pico simulado de matrículas
    let base = finDeSemana ? 18 : 52;
    if (enMatriculas) base *= 1.7;
    const cantidad = Math.round(base * (0.8 + rng() * 0.4));

    let restantesSesion = 0;
    let sesionActual = null;
    for (let i = 0; i < cantidad; i += 1) {
      const hora = elegirPonderado(rng, horas, PESO_HORA);
      const ts = inicioDia + hora * 3600000 + Math.floor(rng() * 3600000);
      if (ts >= ahora.getTime()) continue;

      if (restantesSesion <= 0) {
        sesion += 1;
        sesionActual = `SES-${String(sesion).padStart(6, '0')}`;
        restantesSesion = 1 + Math.floor(rng() * 3);
      }
      restantesSesion -= 1;

      const fueraDominio = rng() < 0.11;
      let texto;
      let intent = null;
      let confianza;
      if (fueraDominio) {
        texto = CONSULTAS_FUERA_DOMINIO[Math.floor(rng() * CONSULTAS_FUERA_DOMINIO.length)];
        confianza = Math.round((0.12 + rng() * 0.4) * 100) / 100;
      } else {
        intent = elegirPonderado(rng, intenciones, pesosIntent);
        texto = intent.ejemplos[Math.floor(rng() * intent.ejemplos.length)];
        const dudosa = rng() < 0.08;
        confianza = dudosa ? 0.38 + rng() * 0.2 : 0.62 + rng() * 0.35;
        confianza = Math.round(confianza * 100) / 100;
      }
      const resuelta = !!intent && confianza >= 0.6;
      const lento = rng() < 0.035;
      const tiempoMs = Math.round(lento ? 3000 + rng() * 1800 : 650 + rng() * 1500 + (resuelta ? 0 : 250));

      consultas.push({
        id: id++,
        sesionId: sesionActual,
        texto,
        intent: resuelta || confianza >= 0.35 ? intent?.nombre ?? null : null,
        categoria: intent?.categoria ?? null,
        confianza,
        resuelta,
        motivoEscalamiento: resuelta ? null : intent ? 'Confianza por debajo del umbral' : 'Fuera del dominio',
        fechaHora: new Date(ts).toISOString(),
        tiempoMs,
        canal: rng() < 0.58 ? 'móvil' : 'web',
      });
    }
  }
  return consultas.sort((a, b) => a.fechaHora.localeCompare(b.fechaHora));
}

function sembrar() {
  const hace20 = new Date(Date.now() - 20 * DIA).toISOString();
  const intenciones = INTENCIONES_SEED.map((it, i) => ({
    id: i + 1,
    nombre: it.nombre,
    categoria: it.categoria,
    descripcion: it.descripcion,
    dependencia: it.dependencia,
    social: !!it.social,
    claves: it.claves,
    ejemplos: it.ejemplos,
    respuestas: it.respuestas.map((texto, j) => ({
      id: (i + 1) * 100 + j,
      texto,
      activa: true,
      fechaActualizacion: hace20,
      actualizadoPor: 2,
    })),
  }));
  return {
    version: VERSION,
    intenciones,
    usuarios: USUARIOS_SEED.map((u) => ({ ...u })),
    configuracion: JSON.parse(JSON.stringify(CONFIG_DEFECTO)),
    consultas: generarHistorial(intenciones),
    sesiones: {},
    auditoria: [],
  };
}

export function getDb() {
  if (db) return db;
  const guardada = storage.get(KEY);
  db = guardada && guardada.version === VERSION ? guardada : sembrar();
  if (!guardada || guardada.version !== VERSION) guardarDb();
  return db;
}

export function guardarDb() {
  if (db) storage.set(KEY, db);
}

export function reiniciarDb() {
  db = sembrar();
  guardarDb();
  return db;
}

export function siguienteId(lista) {
  return lista.reduce((max, x) => Math.max(max, x.id), 0) + 1;
}

export function auditar(usuario, accion, detalle) {
  const d = getDb();
  d.auditoria.unshift({
    id: siguienteId(d.auditoria),
    fecha: new Date().toISOString(),
    usuario: usuario?.correo ?? 'sistema',
    accion,
    detalle,
  });
  d.auditoria = d.auditoria.slice(0, 200);
}

// Simula la latencia de red + PLN (RNF-01: el objetivo es ≤ 3 s).
export function esperar(min = 350, max = 900) {
  return new Promise((r) => setTimeout(r, min + Math.random() * (max - min)));
}
