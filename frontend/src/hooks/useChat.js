import { useCallback, useEffect, useRef, useState } from 'react';
import { REGLAS } from '../config.js';
import { chatService } from '../services/chatService.js';
import { MENSAJES_ERROR } from '../services/errors.js';
import { TEMAS_RAPIDOS } from '../services/mock/seedData.js';
import { sessionStore } from '../utils/storage.js';
import { uuid } from '../utils/id.js';

const STORE_KEY = 'uptc-av-chat';
const BIENVENIDA =
  '¡Hola! Soy el Asistente Virtual de la UPTC. Estoy disponible las 24 horas para orientarte sobre admisiones, matrículas, calendario académico, correo institucional y trámites. Escribe tu pregunta con tus propias palabras o elige un tema:';

const msg = (autor, datos) => ({ id: uuid(), autor, fecha: new Date().toISOString(), ...datos });
const canalActual = () =>
  typeof window !== 'undefined' && window.matchMedia?.('(max-width: 640px)').matches ? 'móvil' : 'web';

/**
 * Lógica de la conversación: sesión anónima (UC-04), envío (UC-02), respuesta o escalamiento
 * (UC-07 / UC-11), errores con reintento (RNF-08) y cierre por inactividad (UC-09).
 */
export function useChat() {
  const guardado = useRef(
    (() => {
      const g = sessionStore.get(STORE_KEY);
      // Una sesión guardada que superó el tiempo de inactividad ya no se reutiliza.
      if (g?.ultimaActividad && Date.now() - g.ultimaActividad > REGLAS.INACTIVIDAD_SESION_MIN * 60000) return null;
      return g;
    })(),
  ).current;
  const [sesionId, setSesionId] = useState(guardado?.sesionId ?? null);
  const [mensajes, setMensajes] = useState(guardado?.mensajes ?? []);
  const [escribiendo, setEscribiendo] = useState(false);
  const [iniciando, setIniciando] = useState(false);
  const [temas, setTemas] = useState(TEMAS_RAPIDOS);
  const ultimaActividad = useRef(guardado?.ultimaActividad ?? Date.now());
  const cerradaPorInactividad = useRef(false);
  const sesionRef = useRef(sesionId);
  sesionRef.current = sesionId;

  const agregar = useCallback((m) => setMensajes((prev) => [...prev, m]), []);
  const tocar = () => {
    ultimaActividad.current = Date.now();
  };

  // Persistencia en la misma pestaña (UC-04 S1: se reutiliza la sesión y su historial).
  useEffect(() => {
    sessionStore.set(STORE_KEY, {
      sesionId,
      mensajes: mensajes.slice(-100),
      ultimaActividad: ultimaActividad.current,
    });
  }, [sesionId, mensajes]);

  const crearSesion = useCallback(async () => {
    const r = await chatService.iniciarSesion(canalActual());
    setSesionId(r.sesionId);
    sesionRef.current = r.sesionId;
    return r.sesionId;
  }, []);

  const iniciar = useCallback(async () => {
    if (sesionRef.current || iniciando) return;
    setIniciando(true);
    try {
      await crearSesion();
      const aviso = cerradaPorInactividad.current
        ? `Tu conversación anterior se cerró después de ${REGLAS.INACTIVIDAD_SESION_MIN} minutos de inactividad.\n\n`
        : '';
      cerradaPorInactividad.current = false;
      setMensajes([msg('bot', { tipo: 'bienvenida', texto: aviso + BIENVENIDA })]);
      tocar();
      chatService
        .obtenerTemas()
        .then((t) => Array.isArray(t) && t.length && setTemas(t))
        .catch(() => {});
    } catch {
      setMensajes([
        msg('bot', {
          tipo: 'error',
          texto: 'No pude iniciar la conversación. Recarga la página o intenta de nuevo en unos segundos.',
          reintentable: 'sesion',
        }),
      ]);
    } finally {
      setIniciando(false);
    }
  }, [crearSesion, iniciando]);

  const procesar = useCallback(
    async (texto) => {
      setEscribiendo(true);
      tocar();
      try {
        const sid = sesionRef.current ?? (await crearSesion());
        const r = await chatService.enviarConsulta(sid, texto);
        agregar(
          msg('bot', {
            tipo: r.tipo === 'escalamiento' ? 'escalamiento' : 'respuesta',
            texto: r.texto,
            dependencia: r.dependencia ?? null,
            intent: r.intent ?? null,
            confianza: r.confianza ?? null,
            consultaOriginal: texto,
          }),
        );
      } catch (e) {
        const codigo = e?.codigo ?? 'SERVIDOR';
        agregar(
          msg('bot', {
            tipo: 'error',
            texto: MENSAJES_ERROR[codigo] ?? e?.message ?? MENSAJES_ERROR.SERVIDOR,
            dependencia: e?.detalles?.dependencia ?? null,
            reintentable: ['RED', 'TIMEOUT', 'SERVIDOR'].includes(codigo) ? 'consulta' : false,
            consultaOriginal: texto,
          }),
        );
      } finally {
        setEscribiendo(false);
        tocar();
      }
    },
    [agregar, crearSesion],
  );

  const enviar = useCallback(
    (texto) => {
      const limpio = String(texto || '').trim();
      if (!limpio || limpio.length > REGLAS.MAX_CARACTERES || escribiendo) return false;
      agregar(msg('usuario', { tipo: 'consulta', texto: limpio }));
      procesar(limpio);
      return true;
    },
    [agregar, procesar, escribiendo],
  );

  const reintentar = useCallback(
    (m) => {
      setMensajes((prev) => prev.filter((x) => x.id !== m.id));
      if (m.reintentable === 'sesion') {
        sesionRef.current = null;
        iniciar();
      } else if (m.consultaOriginal) {
        procesar(m.consultaOriginal);
      }
    },
    [iniciar, procesar],
  );

  const finalizar = useCallback(() => {
    const sid = sesionRef.current;
    if (sid) chatService.cerrarSesion(sid).catch(() => {});
    sesionRef.current = null;
    setSesionId(null);
    setMensajes([]);
    setEscribiendo(false);
  }, []);

  // UC-09: cierre automático tras el tiempo de inactividad.
  useEffect(() => {
    const id = setInterval(() => {
      if (!sesionRef.current) return;
      if (Date.now() - ultimaActividad.current > REGLAS.INACTIVIDAD_SESION_MIN * 60000) {
        cerradaPorInactividad.current = true;
        finalizar();
      }
    }, 30000);
    return () => clearInterval(id);
  }, [finalizar]);

  return { sesionId, mensajes, escribiendo, iniciando, temas, iniciar, enviar, reintentar, finalizar, tocar };
}
