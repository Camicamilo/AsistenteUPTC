import { useEffect, useState } from 'react';
import BarList from '../../components/charts/BarList.jsx';
import ColumnChart from '../../components/charts/ColumnChart.jsx';
import StatTile from '../../components/charts/StatTile.jsx';
import { useAuth } from '../../hooks/useAuth.jsx';
import { adminService } from '../../services/adminService.js';
import { formatDay, formatDecimal, formatNumber, formatPercent, formatSeconds } from '../../utils/format.js';

const RANGOS = [
  { id: '7d', texto: 'Últimos 7 días' },
  { id: '30d', texto: 'Últimos 30 días' },
];

const SERIES = [
  { clave: 'resueltas', nombre: 'Resueltas', color: 'var(--series-1)' },
  { clave: 'escaladas', nombre: 'Escaladas', color: 'var(--series-2)' },
];

// MetricsDashboard (UC-15): consultas, % resueltas vs. escaladas, tiempo de respuesta y top de intenciones.
export default function DashboardPage() {
  const { manejarError } = useAuth();
  const [rango, setRango] = useState('7d');
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let vigente = true;
    setCargando(true);
    setError('');
    adminService
      .metricas(rango)
      .then((d) => vigente && setDatos(d))
      .catch((e) => {
        manejarError(e);
        if (vigente) setError('No se pudieron cargar las métricas.');
      })
      .finally(() => vigente && setCargando(false));
    return () => {
      vigente = false;
    };
  }, [rango, manejarError]);

  const variacion = datos?.totalAnterior ? (datos.total - datos.totalAnterior) / datos.totalAnterior : null;
  const difTasa = datos ? (datos.tasaResolucion - datos.tasaResolucionAnterior) * 100 : 0;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Dashboard</h1>
          <p className="page-sub">Uso y desempeño del asistente virtual</p>
        </div>
        <div className="segmented" role="group" aria-label="Período">
          {RANGOS.map((r) => (
            <button key={r.id} type="button" aria-pressed={rango === r.id} onClick={() => setRango(r.id)}>
              {r.texto}
            </button>
          ))}
        </div>
      </div>

      {error && <p className="notice notice-critical">{error}</p>}
      {!datos && cargando && <p className="empty">Cargando métricas…</p>}

      {datos && (
        <div className={`dash ${cargando ? 'is-loading' : ''}`}>
          <div className="stats">
            <StatTile
              label="Consultas"
              value={formatNumber(datos.total)}
              delta={variacion ?? undefined}
              subirEsBueno={null}
              deltaTexto={variacion != null ? `${formatPercent(Math.abs(variacion))} vs. período anterior` : ''}
            />
            <StatTile
              label="Resueltas sin ayuda humana"
              value={formatPercent(datos.tasaResolucion)}
              delta={Math.round(difTasa * 10) / 10}
              deltaTexto={`${formatDecimal(Math.abs(difTasa))} pts vs. período anterior`}
            />
            <StatTile
              label="Tiempo promedio de respuesta"
              value={formatSeconds(datos.tiempoPromedioMs)}
              sub={`${formatPercent(datos.porcentajeBajo3s)} en ≤ 3 s (meta: 95 %)`}
            />
            <StatTile
              label="Disponibilidad"
              value={formatPercent(datos.disponibilidad)}
              badge={
                <span className={`badge ${datos.disponibilidad >= datos.slaDisponibilidad ? 'badge-good' : 'badge-critical'}`}>
                  {datos.disponibilidad >= datos.slaDisponibilidad ? 'Cumple' : 'No cumple'} el mínimo del{' '}
                  {formatPercent(datos.slaDisponibilidad).replace(',0', '')}
                </span>
              }
            />
          </div>

          <div className="cards">
            <section className="card card-wide" aria-labelledby="ch-dia">
              <h2 id="ch-dia">Consultas por día</h2>
              <p className="card-sub">
                {formatNumber(datos.resueltas)} resueltas · {formatNumber(datos.escaladas)} escaladas a atención humana
              </p>
              <ColumnChart
                datos={datos.serie.map((s) => ({
                  etiqueta: formatDay(s.inicio),
                  resueltas: s.resueltas,
                  escaladas: s.escaladas,
                }))}
                series={SERIES}
                cadaEtiqueta={rango === '30d' ? 5 : 1}
              />
            </section>

            <section className="card" aria-labelledby="ch-top">
              <h2 id="ch-top">Intenciones más consultadas</h2>
              <p className="card-sub">Top 5 de consultas resueltas</p>
              <BarList datos={datos.topIntenciones.map((t) => ({ etiqueta: t.nombre, valor: t.total }))} />
            </section>
          </div>
        </div>
      )}
    </div>
  );
}
