// Implementación SIMULADA del servicio de chat (modo demostración).
import { REGLAS } from '../../config.js';
import { ServiceError } from '../errors.js';
import { clasificar } from './mockNlu.js';
import { esperar, getDb, guardarDb, siguienteId } from './mockDb.js';
import { TEMAS_RAPIDOS } from './seedData.js';

function nuevoIdSesion() {
  const parte = Math.random().toString(36).slice(2, 8).toUpperCase();
  return `SES-${new Date().getFullYear()}-${parte}`;
}

// UC-10: el registro es asíncrono para no afectar el tiempo de respuesta.
function registrarInteraccion(registro) {
  setTimeout(() => {
    const db = getDb();
    db.consultas.push({ id: siguienteId(db.consultas.slice(-50)), ...registro });
    const s = db.sesiones[registro.sesionId];
    if (s) s.totalConsultas += 1;
    guardarDb();
  }, 0);
}

export const mockChat = {
  async iniciarSesion(canal = 'web') {
    await esperar(120, 300);
    const db = getDb();
    const sesionId = nuevoIdSesion();
    db.sesiones[sesionId] = {
      sesionId,
      fechaInicio: new Date().toISOString(),
      fechaFin: null,
      totalConsultas: 0,
      canal,
    };
    guardarDb();
    return { sesionId, fechaInicio: db.sesiones[sesionId].fechaInicio, modoDemo: true };
  },

  async enviarConsulta(sesionId, texto) {
    const limpio = String(texto || '').trim();
    if (!limpio) throw new ServiceError('VALIDACION', 'La consulta está vacía');
    if (limpio.length > REGLAS.MAX_CARACTERES)
      throw new ServiceError('VALIDACION', `La consulta supera ${REGLAS.MAX_CARACTERES} caracteres`);

    const inicio = performance.now();
    const db = getDb();
    const cfg = db.configuracion;
    await esperar(450, 1300);

    const base = {
      sesionId,
      texto: limpio,
      fechaHora: new Date().toISOString(),
      canal: db.sesiones[sesionId]?.canal ?? 'web',
    };

    if (cfg.simularFalloPln) {
      registrarInteraccion({
        ...base,
        intent: null,
        categoria: null,
        confianza: null,
        resuelta: false,
        motivoEscalamiento: 'Motor PLN no disponible',
        tiempoMs: Math.round(performance.now() - inicio),
      });
      throw new ServiceError('PLN_NO_DISPONIBLE', 'Motor PLN no disponible', {
        dependencia: cfg.dependencias.general,
      });
    }

    const { intent, confianza, entidades } = clasificar(limpio, db.intenciones);
    const activas = intent ? intent.respuestas.filter((r) => r.activa) : [];
    const resuelta = !!intent && confianza >= cfg.umbralConfianza && activas.length > 0;
    const dependencia = cfg.dependencias[intent?.dependencia] ?? cfg.dependencias.general;
    const tiempoMs = Math.round(performance.now() - inicio);

    let motivo = null;
    if (!resuelta) {
      if (!intent) motivo = 'Fuera del dominio';
      else if (!activas.length) motivo = 'Sin respuesta activa';
      else motivo = 'Confianza por debajo del umbral';
    }

    registrarInteraccion({
      ...base,
      intent: intent?.nombre ?? null,
      categoria: intent?.categoria ?? null,
      confianza,
      resuelta,
      motivoEscalamiento: motivo,
      tiempoMs,
    });

    if (resuelta) {
      const respuesta = activas[Math.floor(Math.random() * activas.length)];
      return {
        tipo: 'respuesta',
        texto: respuesta.texto,
        intent: intent.nombre,
        categoria: intent.categoria,
        confianza,
        entidades,
        dependencia: null,
        tiempoMs,
      };
    }

    const intro =
      motivo === 'Fuera del dominio'
        ? 'Esa consulta parece estar fuera de los temas que manejo (admisiones, matrículas, calendario académico, correo institucional y trámites). '
        : '';
    return {
      tipo: 'escalamiento',
      texto: intro + cfg.plantillaEscalamiento.replace('{dependencia}', dependencia.nombre),
      intent: intent?.nombre ?? null,
      categoria: intent?.categoria ?? null,
      confianza,
      entidades,
      dependencia,
      tiempoMs,
    };
  },

  async cerrarSesion(sesionId) {
    const db = getDb();
    const s = db.sesiones[sesionId];
    if (s && !s.fechaFin) {
      s.fechaFin = new Date().toISOString();
      guardarDb();
    }
    return { ok: true };
  },

  async obtenerTemas() {
    return TEMAS_RAPIDOS;
  },
};
