import Icon from '../Icon.jsx';

// Tarjeta de escalamiento a atención humana (RF-05 / UC-11): dependencia, correo y enlace.
export default function EscalationCard({ dependencia, consulta, onReformular }) {
  if (!dependencia) return null;
  const asunto = encodeURIComponent('Consulta desde el Asistente Virtual UPTC');
  const cuerpo = encodeURIComponent(`Hola, escribo porque el asistente virtual no pudo resolver mi consulta:\n\n"${consulta || ''}"\n\nGracias.`);
  return (
    <div className="av-escalation">
      <p className="av-escalation-title">
        <Icon name="users" size={16} /> {dependencia.nombre}
      </p>
      <ul className="av-escalation-list">
        {dependencia.correo && (
          <li>
            <Icon name="mail" size={15} />
            <a href={`mailto:${dependencia.correo}`}>{dependencia.correo}</a>
          </li>
        )}
        {dependencia.telefono && (
          <li>
            <Icon name="phone" size={15} />
            <a href={`tel:${dependencia.telefono.replace(/[^\d+]/g, '')}`}>{dependencia.telefono}</a>
          </li>
        )}
        {dependencia.horario && (
          <li>
            <Icon name="clock" size={15} />
            <span>{dependencia.horario}</span>
          </li>
        )}
        {dependencia.enlace && (
          <li>
            <Icon name="link" size={15} />
            <a href={dependencia.enlace} target="_blank" rel="noopener noreferrer">
              Más información<span className="sr-only"> (se abre en una pestaña nueva)</span>
            </a>
          </li>
        )}
      </ul>
      <div className="av-escalation-actions">
        {dependencia.correo && (
          <a className="av-btn av-btn-dark" href={`mailto:${dependencia.correo}?subject=${asunto}&body=${cuerpo}`}>
            <Icon name="mail" size={15} /> Enviar mi consulta por correo
          </a>
        )}
        {onReformular && (
          <button type="button" className="av-btn av-btn-ghost" onClick={onReformular}>
            <Icon name="edit" size={15} /> Reformular
          </button>
        )}
      </div>
    </div>
  );
}
