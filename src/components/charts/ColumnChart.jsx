import { useState } from 'react';
import { formatNumber } from '../../utils/format.js';

function escalaY(max) {
  if (max <= 0) return { tope: 4, ticks: [0, 1, 2, 3, 4] };
  const paso0 = max / 4;
  const mag = 10 ** Math.floor(Math.log10(paso0));
  const paso = [1, 2, 5, 10].map((m) => m * mag).find((p) => p >= paso0);
  const tope = Math.ceil(max / paso) * paso;
  const ticks = [];
  for (let v = 0; v <= tope + 1e-9; v += paso) ticks.push(Math.round(v));
  return { tope, ticks };
}

/**
 * Columnas apiladas de 2 series (resueltas / escaladas) con tooltip al pasar el mouse o enfocar.
 * datos: [{ etiqueta, resueltas, escaladas }]
 */
export default function ColumnChart({ datos, series, cadaEtiqueta = 1, alto = 220 }) {
  const [activo, setActivo] = useState(null);
  const totales = datos.map((d) => series.reduce((s, x) => s + d[x.clave], 0));
  const { tope, ticks } = escalaY(Math.max(...totales, 0));

  return (
    <div className="colchart">
      <ul className="chart-legend" aria-hidden="true">
        {series.map((s) => (
          <li key={s.clave}>
            <span className="chart-swatch" style={{ background: s.color }} />
            {s.nombre}
          </li>
        ))}
      </ul>

      <div className="colchart-body" style={{ height: alto }}>
        <div className="colchart-yaxis" aria-hidden="true">
          {ticks.map((t) => (
            <span key={t} style={{ bottom: `${(t / tope) * 100}%` }}>
              {formatNumber(t)}
            </span>
          ))}
        </div>
        <div className="colchart-plot">
          {ticks.map((t) => (
            <div key={t} className={`colchart-grid ${t === 0 ? 'is-base' : ''}`} style={{ bottom: `${(t / tope) * 100}%` }} />
          ))}
          <div className="colchart-cols">
            {datos.map((d, i) => (
              <div
                key={d.etiqueta}
                className={`colchart-col ${activo === i ? 'is-active' : ''}`}
                tabIndex={0}
                onMouseEnter={() => setActivo(i)}
                onMouseLeave={() => setActivo(null)}
                onFocus={() => setActivo(i)}
                onBlur={() => setActivo(null)}
                aria-label={`${d.etiqueta}: ${series.map((s) => `${d[s.clave]} ${s.nombre.toLowerCase()}`).join(', ')}`}
              >
                <div className="colchart-stack" style={{ height: `${(totales[i] / tope) * 100}%` }}>
                  {series.map((s) =>
                    d[s.clave] > 0 ? (
                      <div
                        key={s.clave}
                        className="colchart-seg"
                        style={{ flexGrow: d[s.clave], background: s.color }}
                      />
                    ) : null,
                  )}
                </div>
                {activo === i && (
                  <div className={`chart-tip ${i > datos.length * 0.65 ? 'is-left' : ''}`} role="tooltip">
                    <strong>{d.etiqueta}</strong>
                    {series.map((s) => (
                      <span key={s.clave} className="chart-tip-row">
                        <span className="chart-tip-key" style={{ background: s.color }} />
                        <b>{formatNumber(d[s.clave])}</b> {s.nombre.toLowerCase()}
                      </span>
                    ))}
                    <span className="chart-tip-total">Total: {formatNumber(totales[i])}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="colchart-xaxis" aria-hidden="true">
        {datos.map((d, i) => (
          <span key={d.etiqueta}>{i % cadaEtiqueta === 0 || i === datos.length - 1 ? d.etiqueta : ''}</span>
        ))}
      </div>
    </div>
  );
}
