import { useState } from 'react';
import { useAuth } from '../../hooks/useAuth.jsx';
import { adminService } from '../../services/adminService.js';
import { CATEGORIAS, LISTA_DEPENDENCIAS } from './constantes.js';

// Formulario para crear una intención con su primera respuesta (UC-12).
export default function NewIntentForm({ onCreada }) {
  const { manejarError } = useAuth();
  const [form, setForm] = useState({
    nombre: '',
    categoria: CATEGORIAS[0],
    dependencia: 'general',
    descripcion: '',
    ejemplos: '',
    respuesta: '',
  });
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const set = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setGuardando(true);
    try {
      const it = await adminService.crearIntencion({ ...form, ejemplos: form.ejemplos.split('\n') });
      onCreada(it);
    } catch (err) {
      manejarError(err);
      setError({ campo: err.detalles?.campo, mensaje: err.message });
    } finally {
      setGuardando(false);
    }
  };

  const err = (campo) => (error?.campo === campo ? error.mensaje : null);

  return (
    <form className="editor" onSubmit={onSubmit} noValidate>
      {error && !error.campo && <p className="notice notice-critical">{error.mensaje}</p>}
      <div className="field">
        <label htmlFor="ni-nombre">Nombre de la intención *</label>
        <input
          id="ni-nombre"
          className="input"
          placeholder="ej.: consulta_matricula"
          value={form.nombre}
          onChange={set('nombre')}
          aria-invalid={!!err('nombre')}
        />
        {err('nombre') ? (
          <span className="field-error">{err('nombre')}</span>
        ) : (
          <span className="field-hint">Minúsculas y guion bajo, sin espacios.</span>
        )}
      </div>
      <div className="grid-2">
        <div className="field">
          <label htmlFor="ni-cat">Categoría *</label>
          <select id="ni-cat" className="select" value={form.categoria} onChange={set('categoria')}>
            {CATEGORIAS.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="ni-dep">Dependencia (si se escala)</label>
          <select id="ni-dep" className="select" value={form.dependencia} onChange={set('dependencia')}>
            {LISTA_DEPENDENCIAS.map((d) => (
              <option key={d.clave} value={d.clave}>
                {d.nombre}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="field">
        <label htmlFor="ni-desc">Descripción</label>
        <input id="ni-desc" className="input" value={form.descripcion} onChange={set('descripcion')} />
      </div>
      <div className="field">
        <label htmlFor="ni-ej">Preguntas de ejemplo (una por línea)</label>
        <textarea
          id="ni-ej"
          className="textarea"
          placeholder={'¿Cómo pido un paz y salvo?\nNecesito el paz y salvo de biblioteca'}
          value={form.ejemplos}
          onChange={set('ejemplos')}
        />
        <span className="field-hint">Sirven para entrenar al motor PLN con formas distintas de preguntar lo mismo.</span>
      </div>
      <div className="field">
        <label htmlFor="ni-resp">Respuesta oficial *</label>
        <textarea
          id="ni-resp"
          className="textarea"
          value={form.respuesta}
          onChange={set('respuesta')}
          aria-invalid={!!err('texto')}
        />
        {err('texto') && <span className="field-error">{err('texto')}</span>}
      </div>
      <div>
        <button type="submit" className="btn btn-primary" disabled={guardando}>
          {guardando ? 'Guardando…' : 'Crear intención'}
        </button>
      </div>
    </form>
  );
}
