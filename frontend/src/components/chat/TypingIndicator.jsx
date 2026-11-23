// Tres puntos animados mientras el asistente procesa (RF-01: aparece en menos de 1 s).
export default function TypingIndicator() {
  return (
    <div className="av-row av-row-bot">
      <div className="av-avatar" aria-hidden="true">AV</div>
      <div className="av-bubble av-bubble-bot av-typing" role="status">
        <span className="av-dot" />
        <span className="av-dot" />
        <span className="av-dot" />
        <span className="sr-only">El asistente está escribiendo…</span>
      </div>
    </div>
  );
}
