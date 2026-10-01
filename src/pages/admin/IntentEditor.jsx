import { useEffect, useState } from 'react';
import Icon from '../../components/Icon.jsx';
import { useAuth } from '../../hooks/useAuth.jsx';
import { adminService } from '../../services/adminService.js';
import { formatDateTime } from '../../utils/format.js';
import { CATEGORIAS, LISTA_DEPENDENCIAS } from './constantes.js';

function Respuesta({ intentId, r, onGuardado }) {
  const { manejarError } = useAuth();
  const [texto, setTexto] = useState(r.texto);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const cambiado = texto.trim() !== r.texto;
  const max = adminService.MAX_RESPUESTA;

  const guardar = async (cambios, mensaje) => {
    setGuardando(true);
    setError('');
    try {
      const it = await adminService.actualizarRespuesta(intentId, r.id, cambios);
      onGuardado(it, mensaje);
    } catch (e) {
      manejarError(e);
      setError(e.message);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className={`resp ${r.activa ? '' : 'is-off'}`}>
      <div className="resp-head">
        <span className={`badge ${r.activa ? 'badge-good' : 'badge-neutral'}`}>{r.activa ? 'Activa' : 'Inactiva'}</span>
        <span className="cell-muted">Actualizada {formatDateTime(r.fechaActualizacion)}</span>
      </div>
      <label className="sr-only" htmlFor={`resp-${r.id}`}>
        Texto de la respuesta
      </label>
      <textarea
        id={`resp-${r.id}`}
        className="textarea"
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        aria-invalid={texto.length > max || !texto.trim()}
      />
      <div className="resp-actions">
        <span className={`field-hint ${texto.length > max ? 'field-error' : ''}`}>
          {texto.length}/{max}
        </span>
        {error && <span className="field-error">{error}</span>}
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          disabled={guardando}
          onClick={() =>
            guardar({ activa: !r.activa }, r.activa ? 'Respuesta desactivada (se conserva en el historial).' : 'Respuesta activada.')
          }
        >
          {r.activa ? 'Desactivar' : 'Activar'}
        </button>
        <button
          type="button"
          className="btn btn-dark btn-sm"
          disabled={!cambiado || guardando || !texto.trim() || texto.length > max}
          onClick={() => guardar({ texto }, 'Respuesta actualizada. El chat ya usa el nuevo texto.')}
        >
          Guardar
        </button>
      </div>
    </div>
  );
}

// Detalle de una intención: datos generales, respuestas (editar / activar / desactivar) y nueva respuesta.
export default function IntentEditor({ id, onCambio }) {
  const { manejarError } = useAuth();
  const [it, setIt] = useState(null);
  const [general, setGeneral] = useState(null);
  const [nueva, setNueva] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    adminService
      .obtenerIntencion(id)
      .then((d) => {
        setIt(d);
        setGeneral({ categoria: d.categoria, dependencia: d.dependencia, descripcion: d.descripcion || '' });
      })
      .catch((e) => {
        manejarError(e);
        setError('No se pudo cargar la intención.');
      });
  }, [id, manejarError]);

  if (error && !it) return <p className="notice notice-critical">{error}</p>;
  if (!it) return <p className="empty">Cargando…</p>;

  const actualizar = (d, mensaje) => {
    setIt(d);
    onCambio(mensaje);
  };

  const guardarGeneral = async () => {
    setOcupado(true);
    setError('');
    try {
      actualizar(await adminService.actualizarIntencion(it.id, general), 'Datos de la intención guardados.');
    } catch (e) {
      manejarError(e);
      setError(e.message);
    } finally {
      setOcupado(false);
    }
  };

  const agregar = async () => {
    setOcupado(true);
    setError('');
    try {
      actualizar(await adminService.crearRespuesta(it.id, nueva), 'Respuesta alternativa agregada.');
      setNueva('');
    } catch (e) {
      manejarError(e);
      setError(e.message);
    } finally {
      setOcupado(false);
    }
  };

  const generalCambiado =
    general.categoria !== it.categoria || general.dependencia !== it.dependencia || general.descripcion !== (it.descripcion || '');

  return (
    <div className="editor">
      <div className="editor-title">
        <code className="intent-name">{it.nombre}</code>
        {it.ejemplos?.length > 0 && <p className="cell-sub">Ej.: «{it.ejemplos.slice(0, 2).join('», «')}»</p>}
      </div>

      {error && <p className="notice notice-critical">{error}</p>}

      <div className="grid-2">
        <div className="field">
          <label htmlFor="ed-cat">Categoría</label>
          <select
            id="ed-cat"
            className="select"
            value={general.categoria}
            onChange={(e) => setGeneral((g) => ({ ...g, categoria: e.target.value }))}
          >
            {CATEGORIAS.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="ed-dep">Dependencia (si se escala)</label>
          <select
            id="ed-dep"
            className="select"
            value={general.dependencia}
            onChange={(e) => setGeneral((g) => ({ ...g, dependencia: e.target.value }))}
          >
            {LISTA_DEPENDENCIAS.map((d) => (
              <option key={d.clave} value={d.clave}>
                {d.nombre}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="field">
        <label htmlFor="ed-desc">Descripción</label>
        <input
          id="ed-desc"
          className="input"
          value={general.descripcion}
          onChange={(e) => setGeneral((g) => ({ ...g, descripcion: e.target.value }))}
        />
      </div>
      <div>
        <button type="button" className="btn btn-dark btn-sm" disabled={!generalCambiado || ocupado} onClick={guardarGeneral}>
          Guardar datos
        </button>
      </div>

      <h3 className="editor-h">
        Respuestas · {it.respuestas.filter((r) => r.activa).length} de {it.respuestas.length} activas
      </h3>
      <p className="field-hint">
        Si una intención tiene varias respuestas activas, el asistente alterna entre ellas. Desactivar conserva el texto para
        auditoría.
      </p>
      {it.respuestas.map((r) => (
        <Respuesta key={`${r.id}-${r.fechaActualizacion}`} intentId={it.id} r={r} onGuardado={actualizar} />
      ))}

      <div className="field">
        <label htmlFor="ed-nueva">Agregar respuesta alternativa</label>
        <textarea
          id="ed-nueva"
          className="textarea"
          placeholder="Texto con terminología institucional oficial…"
          value={nueva}
          onChange={(e) => setNueva(e.target.value)}
        />
      </div>
      <div>
        <button type="button" className="btn btn-primary btn-sm" disabled={!nueva.trim() || ocupado} onClick={agregar}>
          <Icon name="plus" size={16} /> Agregar respuesta
        </button>
      </div>
    </div>
  );
}
