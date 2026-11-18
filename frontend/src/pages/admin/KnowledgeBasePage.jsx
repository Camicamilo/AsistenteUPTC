import { useCallback, useEffect, useState } from 'react';
import Drawer from '../../components/admin/Drawer.jsx';
import Icon from '../../components/Icon.jsx';
import { useAuth } from '../../hooks/useAuth.jsx';
import { adminService } from '../../services/adminService.js';
import { formatDateTime } from '../../utils/format.js';
import { CATEGORIAS } from './constantes.js';
import IntentEditor from './IntentEditor.jsx';
import NewIntentForm from './NewIntentForm.jsx';

// KnowledgeBaseManager (UC-12 / UC-13): intenciones y sus respuestas, sin tocar código.
export default function KnowledgeBasePage() {
  const { manejarError } = useAuth();
  const [filtros, setFiltros] = useState({ q: '', categoria: '', page: 1 });
  const [res, setRes] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);
  const [aviso, setAviso] = useState('');

  const cargar = useCallback(() => {
    setCargando(true);
    adminService
      .listarIntenciones({ ...filtros, pageSize: 8 })
      .then(setRes)
      .catch(manejarError)
      .finally(() => setCargando(false));
  }, [filtros, manejarError]);

  useEffect(() => {
    const id = setTimeout(cargar, filtros.q ? 250 : 0);
    return () => clearTimeout(id);
  }, [cargar, filtros.q]);

  const avisar = (texto) => {
    setAviso(texto);
    setTimeout(() => setAviso(''), 3500);
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Base de conocimiento</h1>
          <p className="page-sub">Intenciones que entiende el asistente y las respuestas oficiales que entrega</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setCreando(true)}>
          <Icon name="plus" size={18} /> Nueva intención
        </button>
      </div>

      {aviso && (
        <p className="notice notice-good" role="status">
          <Icon name="check" size={16} /> {aviso}
        </p>
      )}

      <div className="filters">
        <div className="search">
          <Icon name="search" size={16} />
          <label htmlFor="kb-q" className="sr-only">
            Buscar
          </label>
          <input
            id="kb-q"
            className="input"
            placeholder="Buscar intención o respuesta…"
            value={filtros.q}
            onChange={(e) => setFiltros((f) => ({ ...f, q: e.target.value, page: 1 }))}
          />
        </div>
        <label htmlFor="kb-cat" className="sr-only">
          Categoría
        </label>
        <select
          id="kb-cat"
          className="select select-auto"
          value={filtros.categoria}
          onChange={(e) => setFiltros((f) => ({ ...f, categoria: e.target.value, page: 1 }))}
        >
          <option value="">Todas las categorías</option>
          {CATEGORIAS.map((c) => (
            <option key={c} value={c}>
              {c[0].toUpperCase() + c.slice(1)}
            </option>
          ))}
        </select>
      </div>

      <div className={`card card-flush ${cargando ? 'is-loading' : ''}`}>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Intención</th>
                <th scope="col">Categoría</th>
                <th scope="col">Respuestas activas</th>
                <th scope="col">Última actualización</th>
                <th scope="col">
                  <span className="sr-only">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {res?.items.map((it) => (
                <tr key={it.id}>
                  <td>
                    <code className="intent-name">{it.nombre}</code>
                    <span className="cell-sub">{it.descripcion}</span>
                  </td>
                  <td>
                    <span className="badge badge-plain">{it.categoria}</span>
                  </td>
                  <td>
                    {it.respuestasActivas === 0 ? (
                      <span className="badge badge-critical">Ninguna: se escala</span>
                    ) : (
                      <span className="num">
                        {it.respuestasActivas} de {it.totalRespuestas}
                      </span>
                    )}
                  </td>
                  <td className="cell-muted">{it.ultimaActualizacion ? formatDateTime(it.ultimaActualizacion) : '—'}</td>
                  <td className="cell-actions">
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditando(it.id)}>
                      <Icon name="edit" size={15} /> Editar
                    </button>
                  </td>
                </tr>
              ))}
              {res && res.items.length === 0 && (
                <tr>
                  <td colSpan={5} className="empty">
                    No hay intenciones que coincidan con la búsqueda.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {res && res.paginas > 1 && (
          <div className="pager">
            <span>
              {res.total} intenciones · página {res.page} de {res.paginas}
            </span>
            <div>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                disabled={res.page <= 1}
                onClick={() => setFiltros((f) => ({ ...f, page: f.page - 1 }))}
              >
                <Icon name="chevronLeft" size={16} /> Anterior
              </button>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                disabled={res.page >= res.paginas}
                onClick={() => setFiltros((f) => ({ ...f, page: f.page + 1 }))}
              >
                Siguiente <Icon name="chevronRight" size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {editando && (
        <Drawer titulo="Editar intención" onClose={() => setEditando(null)}>
          <IntentEditor
            id={editando}
            onCambio={(texto) => {
              avisar(texto);
              cargar();
            }}
          />
        </Drawer>
      )}

      {creando && (
        <Drawer titulo="Nueva intención" onClose={() => setCreando(false)}>
          <NewIntentForm
            onCreada={(it) => {
              setCreando(false);
              avisar(`Intención «${it.nombre}» creada. Ya está disponible en el chat.`);
              cargar();
            }}
          />
        </Drawer>
      )}
    </div>
  );
}
