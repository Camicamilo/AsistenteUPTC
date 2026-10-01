import { useEffect } from 'react';
import { REGLAS } from '../../config.js';
import Icon from '../Icon.jsx';

// Caja de texto con validación en tiempo real (RF-01 / UC-02): no vacía y máximo 500 caracteres.
export default function InputBox({ value, onChange, onSend, ocupado, sinConexion, inputRef, onToggleTemas, temasAbiertos }) {
  const max = REGLAS.MAX_CARACTERES;
  const largo = value.length;
  const excedido = largo > max;
  const vacio = value.trim().length === 0;
  const bloqueado = sinConexion;
  const puedeEnviar = !vacio && !excedido && !ocupado && !bloqueado;

  // Ajusta la altura del textarea al contenido (máx. ~5 líneas).
  useEffect(() => {
    const el = inputRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 132)}px`;
  }, [value, inputRef]);

  const enviar = () => {
    if (puedeEnviar) onSend(value);
    else inputRef.current?.focus(); // S1: Enter sin texto no envía y conserva el foco
  };

  const onKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      enviar();
    }
  };

  const estadoContador = excedido ? 'av-counter-over' : largo > max * 0.9 ? 'av-counter-near' : '';

  return (
    <form
      className="av-input"
      onSubmit={(e) => {
        e.preventDefault();
        enviar();
      }}
    >
      {excedido && (
        <p id="av-input-error" className="av-input-error" role="alert">
          Máximo {max} caracteres: te sobran {largo - max}.
        </p>
      )}
      <div className={`av-input-row ${excedido ? 'is-invalid' : ''}`}>
        <button
          type="button"
          className={`av-icon-btn ${temasAbiertos ? 'is-active' : ''}`}
          onClick={onToggleTemas}
          aria-expanded={temasAbiertos}
          aria-controls="av-temas-bandeja"
          title="Temas frecuentes"
          disabled={bloqueado}
        >
          <Icon name="sparkles" size={18} />
          <span className="sr-only">Temas frecuentes</span>
        </button>
        <label htmlFor="av-textarea" className="sr-only">
          Escribe tu pregunta
        </label>
        <textarea
          id="av-textarea"
          ref={inputRef}
          rows={1}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder={bloqueado ? 'Sin conexión…' : 'Escribe tu pregunta aquí…'}
          disabled={bloqueado}
          aria-invalid={excedido}
          aria-describedby={`av-counter${excedido ? ' av-input-error' : ''}`}
        />
        <button
          type="button"
          className="av-icon-btn"
          disabled
          title="Próximamente: consultas por voz"
          aria-label="Consulta por voz (próximamente)"
        >
          <Icon name="mic" size={18} />
        </button>
        <button type="submit" className="av-send" disabled={!puedeEnviar} aria-label="Enviar consulta">
          <Icon name="send" size={18} />
        </button>
      </div>
      <div className="av-input-meta">
        <span>Enter para enviar · Shift + Enter para nueva línea</span>
        <span id="av-counter" className={`av-counter ${estadoContador}`}>
          {largo}/{max}
        </span>
      </div>
    </form>
  );
}
