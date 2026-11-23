// Accesos rápidos a temas frecuentes (prototipo 6.2): enviar sin escribir.
export default function QuickTopics({ temas, onSelect, disabled }) {
  if (!temas?.length) return null;
  return (
    <div className="av-topics" role="group" aria-label="Temas frecuentes">
      {temas.map((t) => (
        <button key={t.id} type="button" className="av-chip" onClick={() => onSelect(t.consulta)} disabled={disabled}>
          {t.etiqueta}
        </button>
      ))}
    </div>
  );
}
