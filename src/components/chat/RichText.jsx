// Muestra texto del asistente convirtiendo URLs y correos en enlaces activos (UC-08 S1).
// Se construyen elementos React (nunca innerHTML), así que el texto no puede inyectar HTML.
function urlCorta(url) {
  try {
    const u = new URL(url);
    const host = u.hostname.replace(/^www\./, '');
    const segs = u.pathname.split('/').filter(Boolean);
    if (!segs.length || segs[segs.length - 1] === 'index.html') return host;
    return segs.length === 1 ? `${host}/${segs[0]}` : `${host}/…/${segs[segs.length - 1]}`;
  } catch {
    return url;
  }
}

const PATRON = /(https?:\/\/[^\s<>()]+[^\s<>().,;:!?¿¡»"'])|([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})/g;

function enlazar(linea, claveBase) {
  const partes = [];
  let ultimo = 0;
  let m;
  PATRON.lastIndex = 0;
  while ((m = PATRON.exec(linea)) !== null) {
    if (m.index > ultimo) partes.push(linea.slice(ultimo, m.index));
    if (m[1]) {
      partes.push(
        <a key={`${claveBase}-${m.index}`} href={m[1]} target="_blank" rel="noopener noreferrer">
          {urlCorta(m[1])}
          <span className="sr-only"> (se abre en una pestaña nueva)</span>
        </a>,
      );
    } else {
      partes.push(
        <a key={`${claveBase}-${m.index}`} href={`mailto:${m[2]}`}>
          {m[2]}
        </a>,
      );
    }
    ultimo = m.index + m[0].length;
  }
  if (ultimo < linea.length) partes.push(linea.slice(ultimo));
  return partes;
}

export default function RichText({ text }) {
  const parrafos = String(text || '').split(/\n{2,}/);
  return parrafos.map((p, i) => (
    <p key={i}>
      {p.split('\n').map((linea, j, arr) => (
        <span key={j}>
          {enlazar(linea, `${i}-${j}`)}
          {j < arr.length - 1 && <br />}
        </span>
      ))}
    </p>
  ));
}
