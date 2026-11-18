import { useCallback, useEffect, useState } from 'react';
import Icon from '../../components/Icon.jsx';
import { useAuth } from '../../hooks/useAuth.jsx';
import { adminService } from '../../services/adminService.js';
import { formatDateTime, formatPercent, formatSeconds } from '../../utils/format.js';

const RANGOS = [
  { id: 'hoy', texto: 'Hoy' },
  { id: '7d', texto: '7 días' },
  { id: '30d', texto: '30 días' },
  { id: '90d', texto: '90 días' },
];

// Historial de consultas (UC-15 S1): qué preguntaron, qué entendió el asistente y si lo resolvió.
export default function HistorialPage() {
  const { manejarError } = useAuth();
  const [filtros, setFiltros] = useState({ rango: '7d', estado: '', q: '', page: 1 });
  const [res, setRes] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [exportando, setExportando] = useState(false);
  const [error, setError] = useState('');

  const cargar = useCallback(() => {
    setCargando(true);
    setError('');
    adminService
      .historial({ ...filtros, pageSize: 15 })
      .then(setRes)
      .catch((e) => {
        manejarError(e);
        setError('No se pudo cargar el historial.');
      })
      .finally(() => setCargando(false));
  }, [filtros, manejarError]);

  useEffect(() => {
    const id = setTimeout(cargar, filtros.q ? 300 : 0);
    return () => clearTimeout(id);
  }, [cargar, filtros.q]);

  const set = (campo, valor) => setFiltros((f) => ({ ...f, [campo]: valor, page: 1 }));

  const exportar = async () => {
    setExportando(true);
    try {
      const blob = await adminService.exportarCsv(filtros.rango);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `consultas-asistente-${filtros.rango}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      manejarError(e);
      setError('No se pudo exportar el historial.');
    } finally {
      setExportando(false);
    }
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Historial de consultas</h1>
          <p className="page-sub">Cada interacción registrada, sin datos personales del estudiante (RD-02)</p>
        </div>
        <button type="button" className="btn btn-dark" onClick={exportar} disabled={exportando}>
          <Icon name="download" size={17} /> {exportando ? 'Exportando…' : 'Exportar CSV'}
        </button>
      </div>

      {error && <p className="notice notice-critical">{error}</p>}

      <div className="filters">
        <div className="segmented" role="group" aria-label="Período">
          {RANGOS.map((r) => (
            <button key={r.id} type="button" aria-pressed={filtros.rango === r.id} onClick={() => set('rango', r.id)}>
              {r.texto}
            </button>
          ))}
        </div>
        <label htmlFor="h-estado" className="sr-only">
          Estado
        </label>
        <select
          id="h-estado"
          className="select select-auto"
          value={filtros.estado}
          onChange={(e) => set('estado', e.target.value)}
        >
          <option value="">Resueltas y escaladas</option>
          <option value="resuelta">Solo resueltas</option>
          <option value="escalada">Solo escaladas</option>
        </select>
        <div className="search">
          <Icon name="search" size={16} />
          <label htmlFor="h-q" className="sr-only">
            Buscar
          </label>
          <input
            id="h-q"
            className="input"
            placeholder="Buscar en el texto o la intención…"
            value={filtros.q}
            onChange={(e) => set('q', e.target.value)}
          />
        </div>
      </div>

      <div className={`card card-flush ${cargando ? 'is-loading' : ''}`}>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Consulta</th>
                <th scope="col">Intención detectada</th>
                <th scope="col">Confianza</th>
                <th scope="col">Resultado</th>
                <th scope="col">Tiempo</th>
              </tr>
            </thead>
            <tbody>
              {res?.items.map((c) => (
                <tr key={c.id}>
                  <td className="cell-muted">{formatDateTime(c.fechaHora)}</td>
                  <td>
                    {c.texto}
                    <span className="cell-sub">
                      {c.canal} · {c.sesionId}
                    </span>
                  </td>
                  <td>{c.intent ? <code className="intent-name">{c.intent}</code> : <span className="cell-muted">—</span>}</td>
                  <td className="num">{c.confianza == null ? '—' : formatPercent(c.confianza)}</td>
                  <td>
                    {c.resuelta ? (
                      <span className="badge badge-good">Resuelta</span>
                    ) : (
                      <>
                        <span className="badge badge-warning">Escalada</span>
                        <span className="cell-sub">{c.motivoEscalamiento}</span>
                      </>
                    )}
                  </td>
                  <td className={`num ${c.tiempoMs > 3000 ? 'field-error' : ''}`}>{formatSeconds(c.tiempoMs)}</td>
                </tr>
              ))}
              {res && res.items.length === 0 && (
                <tr>
                  <td colSpan={6} className="empty">
                    No hay consultas en este período con esos filtros.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {res && res.paginas > 1 && (
          <div className="pager">
            <span>
              {res.total} consultas · página {res.page} de {res.paginas}
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
    </div>
  );
}
