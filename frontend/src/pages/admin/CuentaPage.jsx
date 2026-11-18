import { useState } from 'react';
import Icon from '../../components/Icon.jsx';
import { useAuth } from '../../hooks/useAuth.jsx';
import { adminService } from '../../services/adminService.js';
import { formatDateTime } from '../../utils/format.js';

// Mi cuenta: datos de la sesión y cambio de contraseña propia.
export default function CuentaPage() {
  const { usuario, manejarError, marcarContrasenaCambiada } = useAuth();
  const [form, setForm] = useState({ actual: '', nueva: '', repetir: '' });
  const [error, setError] = useState(null);
  const [ok, setOk] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const set = (c) => (e) => setForm((f) => ({ ...f, [c]: e.target.value }));
  const err = (c) => (error?.campo === c ? error.mensaje : null);

  const enviar = async (e) => {
    e.preventDefault();
    setOk(false);
    if (form.nueva !== form.repetir) {
      setError({ campo: 'repetir', mensaje: 'Las contraseñas no coinciden' });
      return;
    }
    setGuardando(true);
    setError(null);
    try {
      await adminService.cambiarContrasena(form.actual, form.nueva);
      setForm({ actual: '', nueva: '', repetir: '' });
      setOk(true);
      marcarContrasenaCambiada();
    } catch (x) {
      manejarError(x);
      setError({ campo: x.detalles?.campo, mensaje: x.message });
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Mi cuenta</h1>
          <p className="page-sub">
            {usuario?.nombre} · {usuario?.correo} · {usuario?.rol}
          </p>
        </div>
      </div>

      <section className="card card-narrow">
        <h2 className="card-title">Cambiar contraseña</h2>
        {usuario?.exp && <p className="field-hint">Tu sesión vence el {formatDateTime(usuario.exp * 1000)}.</p>}
        {ok && (
          <p className="notice notice-good" role="status">
            <Icon name="check" size={16} /> Contraseña actualizada.
          </p>
        )}
        {error && !error.campo && <p className="notice notice-critical">{error.mensaje}</p>}
        <form className="editor" onSubmit={enviar} noValidate>
          <div className="field">
            <label htmlFor="cu-actual">Contraseña actual</label>
            <input
              id="cu-actual"
              className="input"
              type="password"
              autoComplete="current-password"
              value={form.actual}
              onChange={set('actual')}
              aria-invalid={!!err('actual')}
            />
            {err('actual') && <span className="field-error">{err('actual')}</span>}
          </div>
          <div className="field">
            <label htmlFor="cu-nueva">Nueva contraseña</label>
            <input
              id="cu-nueva"
              className="input"
              type="password"
              autoComplete="new-password"
              value={form.nueva}
              onChange={set('nueva')}
              aria-invalid={!!err('nueva')}
            />
            {err('nueva') ? (
              <span className="field-error">{err('nueva')}</span>
            ) : (
              <span className="field-hint">Mínimo 8 caracteres, con letras y números.</span>
            )}
          </div>
          <div className="field">
            <label htmlFor="cu-repetir">Repite la nueva contraseña</label>
            <input
              id="cu-repetir"
              className="input"
              type="password"
              autoComplete="new-password"
              value={form.repetir}
              onChange={set('repetir')}
              aria-invalid={!!err('repetir')}
            />
            {err('repetir') && <span className="field-error">{err('repetir')}</span>}
          </div>
          <div>
            <button type="submit" className="btn btn-primary" disabled={guardando || !form.actual || !form.nueva}>
              {guardando ? 'Guardando…' : 'Cambiar contraseña'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
