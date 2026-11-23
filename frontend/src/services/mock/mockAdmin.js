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
    const token = crearTokenDemo({
      sub: u.id,
      correo: u.correo,
      nombre: u.nombre,
      rol: u.rol,
      debeCambiarPassword: !!u.debeCambiarPassword,
      exp,
    });
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
    const ejemplosLista = [...new Set((ejemplos || []).map((e) => e.trim()).filter(Boolean))];
    if (ejemplosLista.length < 3)
      throw new ServiceError(
        'VALIDACION',
        'Escribe al menos 3 frases de ejemplo (una por línea) para entrenar al asistente',
        { campo: 'ejemplos' },
      );
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

// ---------------------------------------------------------------------------
// Funciones del panel completo (historial, usuarios, configuración, auditoría)
// equivalentes a la API real, para que el modo demostración siga funcionando.
// ---------------------------------------------------------------------------
const ROLES = ['superadministrador', 'administrador'];

function exigirSuper() {
  const u = usuarioActual();
  if (u.rol !== 'superadministrador')
    throw new ServiceError('PROHIBIDO', 'Esta acción solo la puede hacer un superadministrador');
  return u;
}

function contrasenaTemporal() {
  const letras = 'abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ';
  let s = '';
  for (let i = 0; i < 8; i += 1) s += letras[Math.floor(Math.random() * letras.length)];
  return `${s}${Math.floor(10 + Math.random() * 89)}*`;
}

function consultasDelRango(rango) {
  const dias = rango === 'hoy' ? 1 : rango === '30d' ? 30 : rango === '90d' ? 90 : 7;
  const desde = inicioDelDia(Date.now()) - (dias - 1) * DIA;
  return getDb()
    .consultas.filter((c) => Date.parse(c.fechaHora) >= desde)
    .slice()
    .sort((a, b) => b.fechaHora.localeCompare(a.fechaHora));
}

function celdaCsv(v) {
  if (v == null) return '';
  let t = String(v).replace(/[\r\n]+/g, ' ');
  if (/^[=+\-@]/.test(t)) t = `'${t}`;
  return `"${t.replace(/"/g, '""')}"`;
}

Object.assign(mockAdmin, {
  async historial({ rango = '7d', estado = '', q = '', page = 1, pageSize = 15 } = {}) {
    usuarioActual();
    await esperar(150, 350);
    const term = q.trim().toLowerCase();
    const lista = consultasDelRango(rango)
      .filter((c) => !estado || (estado === 'resuelta') === c.resuelta)
      .filter((c) => !term || c.texto.toLowerCase().includes(term) || (c.intent || '').includes(term));
    return paginar(lista, page, pageSize);
  },

  async exportarCsv(rango = '30d') {
    usuarioActual();
    const filas = consultasDelRango(rango).map((c) =>
      [
        c.fechaHora.replace('T', ' ').slice(0, 19),
        celdaCsv(c.sesionId),
        celdaCsv(c.canal),
        celdaCsv(c.texto),
        celdaCsv(c.intent),
        celdaCsv(c.categoria),
        c.confianza == null ? '' : c.confianza.toFixed(2),
        c.resuelta ? 'resuelta' : 'escalada',
        celdaCsv(c.motivoEscalamiento),
        c.tiempoMs,
      ].join(';'),
    );
    const csv = ['fecha_hora;sesion;canal;consulta;intencion;categoria;confianza;estado;motivo_escalamiento;tiempo_ms', ...filas].join('\n');
    return new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
  },

  async historialIntencion(id) {
    usuarioActual();
    const it = getDb().intenciones.find((x) => x.id === Number(id));
    if (!it) throw new ServiceError('VALIDACION', 'La intención no existe');
    const tipos = {
      'Crear intención': 'IntencionCreada',
      'Editar intención': 'IntencionEditada',
      'Agregar respuesta': 'RespuestaCreada',
      'Editar respuesta': 'RespuestaEditada',
      'Activar respuesta': 'RespuestaActivada',
      'Desactivar respuesta': 'RespuestaDesactivada',
    };
    return getDb()
      .auditoria.filter((a) => tipos[a.accion] && a.detalle.split(' ')[0] === it.nombre)
      .slice()
      .reverse()
      .map((a, i) => ({ id: a.id, version: i + 1, tipoEvento: tipos[a.accion], datos: { detalle: a.detalle }, fechaHora: a.fecha, usuarioId: a.usuario }));
  },

  async dependencias() {
    return Object.values(getDb().configuracion.dependencias);
  },

  async cambiarContrasena(actual, nueva) {
    const u = usuarioActual();
    await esperar(200, 400);
    if (u.password !== actual) throw new ServiceError('VALIDACION', 'La contraseña actual no es correcta', { campo: 'actual' });
    if (!nueva || nueva.length < 8 || !/[A-Za-z]/.test(nueva) || !/\d/.test(nueva))
      throw new ServiceError('VALIDACION', 'La contraseña debe tener mínimo 8 caracteres, letras y números', { campo: 'nueva' });
    u.password = nueva;
    u.debeCambiarPassword = false;
    auditar(u, 'Cambiar contraseña', 'Cambio de contraseña propia');
    guardarDb();
    return null;
  },

  async listarUsuarios() {
    exigirSuper();
    await esperar(150, 300);
    return getDb().usuarios.map(publico).sort((a, b) => a.nombre.localeCompare(b.nombre));
  },

  async crearUsuario({ nombre, correo, rol }) {
    const actor = exigirSuper();
    await esperar(250, 500);
    const db = getDb();
    const c = String(correo || '').trim().toLowerCase();
    if (!/^[a-z0-9._%+-]+@uptc\.edu\.co$/.test(c))
      throw new ServiceError('VALIDACION', 'Solo se permiten correos institucionales @uptc.edu.co', { campo: 'correo' });
    if (String(nombre || '').trim().length < 3)
      throw new ServiceError('VALIDACION', 'El nombre debe tener entre 3 y 120 caracteres', { campo: 'nombre' });
    if (!ROLES.includes(rol)) throw new ServiceError('VALIDACION', 'Rol no válido', { campo: 'rol' });
    if (db.usuarios.some((u) => u.correo === c))
      throw new ServiceError('CONFLICTO', 'Ya existe un usuario con ese correo', { campo: 'correo' });
    const temporal = contrasenaTemporal();
    const nuevo = {
      id: siguienteId(db.usuarios),
      nombre: nombre.trim(),
      correo: c,
      password: temporal,
      rol,
      activo: true,
      debeCambiarPassword: true,
      fechaRegistro: new Date().toISOString(),
      ultimoAcceso: null,
    };
    db.usuarios.push(nuevo);
    auditar(actor, 'Crear usuario', `${c} (${rol})`);
    guardarDb();
    return { usuario: publico(nuevo), contrasenaTemporal: temporal };
  },

  async actualizarUsuario(id, { nombre, rol, activo }) {
    const actor = exigirSuper();
    await esperar(200, 400);
    const u = getDb().usuarios.find((x) => x.id === Number(id));
    if (!u) throw new ServiceError('VALIDACION', 'El usuario no existe');
    if (u.id === actor.id && activo === false)
      throw new ServiceError('VALIDACION', 'No puedes desactivar tu propia cuenta', { campo: 'activo' });
    if (u.id === actor.id && rol && rol !== 'superadministrador')
      throw new ServiceError('VALIDACION', 'No puedes quitarte el rol de superadministrador', { campo: 'rol' });
    if (nombre !== undefined) u.nombre = String(nombre).trim();
    if (rol) u.rol = rol;
    if (activo !== undefined) u.activo = !!activo;
    auditar(actor, activo === false ? 'Desactivar usuario' : activo === true ? 'Activar usuario' : 'Editar usuario', u.correo);
    guardarDb();
    return publico(u);
  },

  async restablecerContrasena(id) {
    const actor = exigirSuper();
    await esperar(200, 400);
    const u = getDb().usuarios.find((x) => x.id === Number(id));
    if (!u) throw new ServiceError('VALIDACION', 'El usuario no existe');
    u.password = contrasenaTemporal();
    u.debeCambiarPassword = true;
    auditar(actor, 'Restablecer contraseña', u.correo);
    guardarDb();
    return { contrasenaTemporal: u.password };
  },

  async configuracion() {
    usuarioActual();
    const { umbralConfianza, inactividadSesionMin, plantillaEscalamiento, notificarPorCorreo } = getDb().configuracion;
    return { umbralConfianza, inactividadSesionMin, plantillaEscalamiento, notificarPorCorreo };
  },

  async guardarConfiguracion(datos) {
    const actor = exigirSuper();
    await esperar(200, 400);
    const cfg = getDb().configuracion;
    const umbral = datos.umbralConfianza ?? cfg.umbralConfianza;
    const inactividad = datos.inactividadSesionMin ?? cfg.inactividadSesionMin;
    const plantilla = (datos.plantillaEscalamiento ?? cfg.plantillaEscalamiento).trim();
    if (!(umbral >= REGLAS.UMBRAL_MIN && umbral <= REGLAS.UMBRAL_MAX))
      throw new ServiceError('VALIDACION', 'El umbral de confianza debe estar entre 50 % y 80 %', { campo: 'umbralConfianza' });
    if (!(inactividad >= 5 && inactividad <= 240))
      throw new ServiceError('VALIDACION', 'La inactividad debe estar entre 5 y 240 minutos', { campo: 'inactividadSesionMin' });
    if (!plantilla.includes('{dependencia}') || plantilla.length > 500)
      throw new ServiceError('VALIDACION', 'La plantilla debe incluir {dependencia} y tener máximo 500 caracteres', {
        campo: 'plantillaEscalamiento',
      });
    Object.assign(cfg, {
      umbralConfianza: umbral,
      inactividadSesionMin: inactividad,
      plantillaEscalamiento: plantilla,
      notificarPorCorreo: datos.notificarPorCorreo ?? cfg.notificarPorCorreo,
    });
    auditar(actor, 'Configurar sistema', `umbral ${umbral}, inactividad ${inactividad} min`);
    guardarDb();
    return mockAdmin.configuracion();
  },

  async restaurarConfiguracion() {
    return mockAdmin.guardarConfiguracion({
      umbralConfianza: 0.6,
      inactividadSesionMin: 30,
      plantillaEscalamiento:
        'No tengo información suficiente para responder esa pregunta con seguridad. Te recomiendo comunicarte con {dependencia}.',
      notificarPorCorreo: true,
    });
  },

  async actualizarDependencia(clave, datos) {
    const actor = exigirSuper();
    await esperar(200, 400);
    const deps = getDb().configuracion.dependencias;
    if (!deps[clave]) throw new ServiceError('VALIDACION', 'La dependencia no existe');
    if (String(datos.nombre || '').trim().length < 3)
      throw new ServiceError('VALIDACION', 'Escribe el nombre oficial de la dependencia (RD-04)', { campo: 'nombre' });
    if (datos.enlace && !/^https?:\/\//.test(datos.enlace))
      throw new ServiceError('VALIDACION', 'El enlace debe empezar por https://', { campo: 'enlace' });
    deps[clave] = { ...deps[clave], ...datos, clave };
    auditar(actor, 'Editar dependencia', clave);
    guardarDb();
    return deps[clave];
  },

  async auditoria(limite = 100) {
    exigirSuper();
    return getDb().auditoria.slice(0, limite);
  },

  async estadoPln() {
    return { disponible: !getDb().configuracion.simularFalloPln, circuito: 'cerrado' };
  },

  async reentrenarPln() {
    usuarioActual();
    await esperar(600, 1200);
    return { ok: true };
  },
});
