import { formatNumber } from '../../utils/format.js';

// Barras horizontales de una sola serie con el valor en la punta.
export default function BarList({ datos }) {
  const max = Math.max(...datos.map((d) => d.valor), 1);
  if (!datos.length) return <p className="empty">Sin datos en este período.</p>;
  return (
    <ol className="barlist">
      {datos.map((d) => (
        <li key={d.etiqueta} title={`${d.etiqueta}: ${formatNumber(d.valor)}`}>
          <span className="barlist-label">{d.etiqueta}</span>
          <span className="barlist-track">
            <span className="barlist-fill" style={{ width: `calc((100% - 56px) * ${d.valor / max})` }} />
            <span className="barlist-value">{formatNumber(d.valor)}</span>
          </span>
        </li>
      ))}
    </ol>
  );
}
