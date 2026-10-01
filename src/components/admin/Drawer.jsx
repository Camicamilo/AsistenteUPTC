import { useEffect, useRef } from 'react';
import Icon from '../Icon.jsx';

// Panel lateral para formularios. Esc o clic fuera lo cierran.
export default function Drawer({ titulo, onClose, children }) {
  const ref = useRef(null);
  const cerrar = useRef(onClose);
  cerrar.current = onClose;
  useEffect(() => {
    const anterior = document.activeElement;
    ref.current?.querySelector('input, textarea, select, button')?.focus();
    const onKey = (e) => e.key === 'Escape' && cerrar.current();
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      anterior?.focus?.();
    };
  }, []);

  return (
    <div className="drawer-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section className="drawer" role="dialog" aria-modal="true" aria-label={titulo} ref={ref}>
        <header className="drawer-head">
          <h2>{titulo}</h2>
          <button type="button" className="btn btn-ghost btn-icon" onClick={onClose} aria-label="Cerrar">
            <Icon name="close" size={18} />
          </button>
        </header>
        <div className="drawer-body">{children}</div>
      </section>
    </div>
  );
}
