import Icon from '../Icon.jsx';

// Tarjeta de indicador: etiqueta · valor · variación opcional vs. el período anterior.
export default function StatTile({ label, value, delta, deltaTexto, subirEsBueno = true, sub, badge }) {
  let deltaClase = '';
  if (typeof delta === 'number' && delta !== 0 && subirEsBueno !== null) deltaClase = delta > 0 === subirEsBueno ? 'is-good' : 'is-bad';
  return (
    <div className="stat">
      <p className="stat-label">{label}</p>
      <p className="stat-value">{value}</p>
      {typeof delta === 'number' && (
        <p className={`stat-delta ${deltaClase}`}>
          {delta !== 0 && <Icon name={delta > 0 ? 'arrowUp' : 'arrowDown'} size={14} />}
          {deltaTexto}
        </p>
      )}
      {badge}
      {sub && <p className="stat-sub">{sub}</p>}
    </div>
  );
}
