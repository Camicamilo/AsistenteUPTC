// Implementación SIMULADA del panel administrativo (IF-05). Replica las reglas de negocio
// de los casos de uso UC-12, UC-13 y UC-15 para que el panel se pueda probar sin backend.
import { REGLAS } from '../../config.js';
import { ServiceError } from '../errors.js';
import { TOKEN_KEY } from '../apiClient.js';
import { sessionStore } from '../../utils/storage.js';
import { crearTokenDemo, decodeJwt, tokenVigente } from '../../utils/jwt.js';
import { auditar, esperar, getDb, guardarDb, siguienteId } from './mockDb.js';

const DIA = 24 * 60 * 60 * 1000;
const MAX_RESPUESTA = 1500;

const publico = ({ password, ...u }) => u; // nunca se expone la contraseña

function usuarioActual() {
  const token = sessionStore.get(TOKEN_KEY);
  if (!token || !tokenVigente(token)) throw new ServiceError('NO_AUTORIZADO', 'Sesión expirada');
  const p = decodeJwt(token);
  const u = getDb().usuarios.find((x) => x.id === p.sub && x.activo);
  if (!u) throw new ServiceError('NO_AUTORIZADO', 'Sesión no válida');
  return u;
}

function resumenIntencion(it) {
  const activas = it.respuestas.filter((r) => r.activa).length;
  const ultima = it.respuestas.reduce((m, r) => (r.fechaActualizacion > m ? r.fechaActualizacion : m), '');
  return {
    id: it.id,
    nombre: it.nombre,
    categoria: it.categoria,
    descripcion: it.descripcion,
    dependencia: it.dependencia,
    respuestasActivas: activas,
    totalRespuestas: it.respuestas.length,
    ultimaActualizacion: ultima || null,
  };
}

function paginar(lista, page = 1, pageSize = 10) {
  const total = lista.length;
  const paginas = Math.max(1, Math.ceil(total / pageSize));
  const p = Math.min(Math.max(1, page), paginas);
  return { items: lista.slice((p - 1) * pageSize, p * pageSize), total, page: p, pageSize, paginas };
}

function inicioDelDia(ts) {
  const d = new Date(ts);
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

function diasDeRango(rango) {
  if (rango === 'hoy') return 1;
  if (rango === '30d') return 30;
  return 7;
}

function validarTextoRespuesta(texto) {
  const t = String(texto || '').trim();
  if (!t) throw new ServiceError('VALIDACION', 'El texto de la respuesta es obligatorio', { campo: 'texto' });
  if (t.length > MAX_RESPUESTA)
    throw new ServiceError('VALIDACION', `La respuesta no puede superar ${MAX_RESPUESTA} caracteres`, { campo: 'texto' });
  return t;
}

export const mockAdmin = {
  MAX_RESPUESTA,

  // ---------- Autenticación (JWT de 8 horas) ----------
  async login(correo, password) {
    await esperar(400, 800);
    const u = getDb().usuarios.find(
      (x) => x.correo.toLowerCase() === String(correo).trim().toLowerCase() && x.password === password && x.activo,
    );
    // Mensaje genérico: no revela qué campo falló (prototipo 6.1).
    if (!u) throw new ServiceError('NO_AUTORIZADO', 'Usuario o contraseña incorrectos');
    u.ultimoAcceso = new Date().toISOString();
    auditar(u, 'Inicio de sesión', 'Acceso al panel administrativo');
    guardarDb();
    const exp = Math.floor(Date.now() / 1000) + REGLAS.JWT_HORAS * 3600;
    const token = crearTokenDemo({ sub: u.id, correo: u.correo, nombre: u.nombre, rol: u.rol, exp });
    return { token, usuario: publico(u) };
  },

  // ---------- Métricas (UC-15) ----------
  async metricas(rango = '7d') {
    usuarioActual();
    await esperar(250, 600);
    const db = getDb();
    const ahora = Date.now();
    const dias = diasDeRango(rango);
    const desde = inicioDelDia(ahora) - (dias - 1) * DIA;
    const prevDesde = desde - dias * DIA;
    const prevHasta = ahora - dias * DIA;

    const actuales = [];
    const previas = [];
    db.consultas.forEach((c) => {
      const t = Date.parse(c.fechaHora);
      if (t >= desde && t <= ahora) actuales.push(c);
      else if (t >= prevDesde && t <= prevHasta) previas.push(c);
    });

    const tasa = (l) => (l.length ? l.filter((c) => c.resuelta).length / l.length : 0);
    const resueltas = actuales.filter((c) => c.resuelta).length;
    const tiempos = actuales.map((c) => c.tiempoMs);
    const promedio = tiempos.length ? tiempos.reduce((s, x) => s + x, 0) / tiempos.length : 0;
    const bajo3s = tiempos.length ? tiempos.filter((t) => t <= 3000).length / tiempos.length : 0;

    let serie;
    if (rango === 'hoy') {
      const horaActual = new Date(ahora).getHours();
      serie = Array.from({ length: horaActual + 1 }, (_, h) => ({
        clave: h,
        inicio: desde + h * 3600000,
        resueltas: 0,
        escaladas: 0,
      }));
      actuales.forEach((c) => {
        const s = serie[new Date(c.fechaHora).getHours()];
        if (s) s[c.resuelta ? 'resueltas' : 'escaladas'] += 1;
      });
    } else {
      serie = Array.from({ length: dias }, (_, i) => ({
        clave: i,
        inicio: desde + i * DIA,
        resueltas: 0,
        escaladas: 0,
      }));
      actuales.forEach((c) => {
        const i = Math.floor((inicioDelDia(Date.parse(c.fechaHora)) - desde) / DIA);
        if (serie[i]) serie[i][c.resuelta ? 'resueltas' : 'escaladas'] += 1;
      });
    }

    const sociales = new Set(db.intenciones.filter((i) => i.social).map((i) => i.nombre));
    const conteo = {};
    actuales.forEach((c) => {
      if (c.intent && c.resuelta && !sociales.has(c.intent)) conteo[c.intent] = (conteo[c.intent] || 0) + 1;
    });
    const topIntenciones = Object.entries(conteo)
      .map(([nombre, total]) => ({ nombre, total }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 5);

    return {
      rango,
      granularidad: rango === 'hoy' ? 'hora' : 'dia',
      total: actuales.length,
      totalAnterior: previas.length,
      resueltas,
      escaladas: actuales.length - resueltas,
      tasaResolucion: tasa(actuales),
      tasaResolucionAnterior: tasa(previas),
      tiempoPromedioMs: promedio,
      porcentajeBajo3s: bajo3s,
      sesiones: new Set(actuales.map((c) => c.sesionId)).size,
      disponibilidad: 0.996,
      slaDisponibilidad: 0.95,
      serie,
      topIntenciones,
    };
  },

  // ---------- Base de conocimiento (UC-12 / UC-13) ----------
  async listarIntenciones({ q = '', categoria = '', page = 1, pageSize = 8 } = {}) {
    usuarioActual();
    await esperar(150, 350);
    const term = q.trim().toLowerCase();
    const lista = getDb()
      .intenciones.filter((it) => !categoria || it.categoria === categoria)
      .filter(
        (it) =>
          !term ||
          it.nombre.toLowerCase().includes(term) ||
          (it.descripcion || '').toLowerCase().includes(term) ||
          it.respuestas.some((r) => r.texto.toLowerCase().includes(term)),
      )
      .map(resumenIntencion)
      .sort((a, b) => a.nombre.localeCompare(b.nombre));
    return paginar(lista, page, pageSize);
  },

  async obtenerIntencion(id) {
    usuarioActual();
    await esperar(100, 250);
    const it = getDb().intenciones.find((x) => x.id === Number(id));
    if (!it) throw new ServiceError('VALIDACION', 'La intención no existe');
    return JSON.parse(JSON.stringify(it));
  },

  async crearIntencion({ nombre, categoria, descripcion, dependencia, respuesta, ejemplos }) {
    const u = usuarioActual();
    await esperar(250, 500);
    const db = getDb();
    const n = String(nombre || '').trim();
    if (!/^[a-z][a-z0-9_]{2,79}$/.test(n))
      throw new ServiceError('VALIDACION', 'Usa minúsculas, números y guion bajo (ej.: consulta_matricula)', {
        campo: 'nombre',
      });
    if (db.intenciones.some((x) => x.nombre === n))
      throw new ServiceError('CONFLICTO', 'Ya existe una intención con ese nombre', { campo: 'nombre' });
    if (!categoria) throw new ServiceError('VALIDACION', 'Selecciona una categoría', { campo: 'categoria' });
    const texto = validarTextoRespuesta(respuesta);
    const ejemplosLista = (ejemplos || []).map((e) => e.trim()).filter(Boolean);
    // En modo demo, las palabras de los ejemplos alimentan el clasificador simulado.
    const claves = {};
    ejemplosLista
      .join(' ')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .split(/[^a-zñ]+/)
      .filter((w) => w.length > 3)
      .forEach((w) => {
        claves[w] = 1.5;
      });
    const id = siguienteId(db.intenciones);
    const ahora = new Date().toISOString();
    const nueva = {
      id,
      nombre: n,
      categoria,
      descripcion: String(descripcion || '').trim(),
      dependencia: dependencia || 'general',
      social: false,
      claves,
      ejemplos: ejemplosLista,
      respuestas: [{ id: id * 100, texto, activa: true, fechaActualizacion: ahora, actualizadoPor: u.id }],
    };
    db.intenciones.push(nueva);
    auditar(u, 'Crear intención', n);
    guardarDb();
    return JSON.parse(JSON.stringify(nueva));
  },

  async actualizarIntencion(id, { categoria, descripcion, dependencia }) {
    const u = usuarioActual();
    await esperar(200, 400);
    const it = getDb().intenciones.find((x) => x.id === Number(id));
    if (!it) throw new ServiceError('VALIDACION', 'La intención no existe');
    if (categoria) it.categoria = categoria;
    if (descripcion !== undefined) it.descripcion = String(descripcion).trim();
    if (dependencia) it.dependencia = dependencia;
    auditar(u, 'Editar intención', it.nombre);
    guardarDb();
    return JSON.parse(JSON.stringify(it));
  },

  async crearRespuesta(intentId, texto) {
    const u = usuarioActual();
    await esperar(200, 400);
    const it = getDb().intenciones.find((x) => x.id === Number(intentId));
    if (!it) throw new ServiceError('VALIDACION', 'La intención no existe');
    const t = validarTextoRespuesta(texto);
    const id = it.respuestas.reduce((m, r) => Math.max(m, r.id), it.id * 100) + 1;
    it.respuestas.push({ id, texto: t, activa: true, fechaActualizacion: new Date().toISOString(), actualizadoPor: u.id });
    auditar(u, 'Agregar respuesta', it.nombre);
    guardarDb();
    return JSON.parse(JSON.stringify(it));
  },

  async actualizarRespuesta(intentId, respuestaId, cambios) {
    const u = usuarioActual();
    await esperar(200, 400);
    const it = getDb().intenciones.find((x) => x.id === Number(intentId));
    const r = it?.respuestas.find((x) => x.id === Number(respuestaId));
    if (!r) throw new ServiceError('VALIDACION', 'La respuesta no existe');
    if (cambios.texto !== undefined) r.texto = validarTextoRespuesta(cambios.texto);
    if (cambios.activa !== undefined) r.activa = !!cambios.activa;
    r.fechaActualizacion = new Date().toISOString();
    r.actualizadoPor = u.id;
    const accion =
      cambios.activa === undefined ? 'Editar respuesta' : cambios.activa ? 'Activar respuesta' : 'Desactivar respuesta';
    auditar(u, accion, `${it.nombre} #${r.id}`);
    guardarDb();
    return JSON.parse(JSON.stringify(it));
  },
};
