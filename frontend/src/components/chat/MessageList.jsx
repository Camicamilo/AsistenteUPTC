import { useEffect, useRef } from 'react';
import { formatTime } from '../../utils/format.js';
import Icon from '../Icon.jsx';
import EscalationCard from './EscalationCard.jsx';
import QuickTopics from './QuickTopics.jsx';
import RichText from './RichText.jsx';
import TypingIndicator from './TypingIndicator.jsx';

function Mensaje({ m, temas, onTopic, onRetry, onReformular, ocupado }) {
  const esBot = m.autor === 'bot';
  return (
    <div className={`av-row ${esBot ? 'av-row-bot' : 'av-row-user'}`}>
      {esBot && (
        <div className="av-avatar" aria-hidden="true">
          AV
        </div>
      )}
      <div className="av-msg">
        <span className="sr-only">{esBot ? 'Asistente dice:' : 'Tú dijiste:'}</span>
        <div
          className={`av-bubble ${esBot ? 'av-bubble-bot' : 'av-bubble-user'} ${m.tipo === 'error' ? 'av-bubble-error' : ''}`}
        >
          {m.tipo === 'error' && (
            <span className="av-bubble-icon">
              <Icon name="alert" size={16} />
            </span>
          )}
          <RichText text={m.texto} />
          {m.tipo === 'escalamiento' && (
            <EscalationCard
              dependencia={m.dependencia}
              consulta={m.consultaOriginal}
              onReformular={() => onReformular(m.consultaOriginal)}
            />
          )}
          {m.tipo === 'error' && m.dependencia && <EscalationCard dependencia={m.dependencia} consulta={m.consultaOriginal} />}
          {m.tipo === 'error' && m.reintentable && (
            <button type="button" className="av-btn av-btn-ghost av-retry" onClick={() => onRetry(m)} disabled={ocupado}>
              <Icon name="refresh" size={15} /> Reintentar
            </button>
          )}
        </div>
        {m.tipo === 'bienvenida' && (
          <>
            <p className="av-privacy">
              <Icon name="shield" size={14} /> No compartas datos personales como tu documento o código. Las conversaciones
              se guardan de forma anónima para mejorar el servicio (Ley 1581 de 2012).
            </p>
            <QuickTopics temas={temas} onSelect={onTopic} disabled={ocupado} />
          </>
        )}
        <time className="av-time" dateTime={m.fecha}>
          {formatTime(m.fecha)}
        </time>
      </div>
    </div>
  );
}

export default function MessageList({ mensajes, escribiendo, temas, onTopic, onRetry, onReformular }) {
  const finRef = useRef(null);

  // UC-08 paso 4: desplazamiento automático al mensaje más reciente.
  useEffect(() => {
    finRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [mensajes.length, escribiendo]);

  return (
    <div className="av-messages" role="log" aria-live="polite" aria-relevant="additions" aria-label="Conversación">
      {mensajes.map((m) => (
        <Mensaje
          key={m.id}
          m={m}
          temas={temas}
          onTopic={onTopic}
          onRetry={onRetry}
          onReformular={onReformular}
          ocupado={escribiendo}
        />
      ))}
      {escribiendo && <TypingIndicator />}
      <div ref={finRef} />
    </div>
  );
}
