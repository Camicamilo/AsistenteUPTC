import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import Icon from '../../components/Icon.jsx';
import '../../components/admin/admin.css';
import { config, REGLAS } from '../../config.js';
import { useAuth } from '../../hooks/useAuth.jsx';

// Login del panel (/admin): mensaje de error genérico, sin registro público (prototipo 6.1).
export default function LoginPage() {
  const { usuario, login, motivoSalida } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);

  if (usuario) return <Navigate to={location.state?.desde || '/admin'} replace />;

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!correo.trim() || !password) {
      setError('Ingresa tu correo y tu contraseña.');
      return;
    }
    setError('');
    setCargando(true);
    try {
      await login(correo, password);
      navigate(location.state?.desde || '/admin', { replace: true });
    } catch (err) {
      setError(err.codigo === 'NO_AUTORIZADO' ? 'Usuario o contraseña incorrectos.' : 'No fue posible iniciar sesión. Intenta de nuevo.');
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="login">
      <form className="login-card" onSubmit={onSubmit} noValidate>
        <span className="adm-mark adm-mark-lg" aria-hidden="true">
          <Icon name="chat" size={24} />
        </span>
        <h1>Panel administrativo</h1>
        <p className="login-sub">Asistente Virtual UPTC · acceso solo para personal autorizado</p>

        {motivoSalida && <p className="notice notice-warning">{motivoSalida}</p>}
        {error && (
          <p className="notice notice-critical" role="alert">
            {error}
          </p>
        )}

        <div className="field">
          <label htmlFor="correo">Correo institucional</label>
          <input
            id="correo"
            className="input"
            type="email"
            autoComplete="username"
            placeholder={`usuario${REGLAS.DOMINIO_ADMIN}`}
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <input
            id="password"
            className="input"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button type="submit" className="btn btn-dark login-btn" disabled={cargando}>
          {cargando ? 'Ingresando…' : 'Ingresar'}
        </button>

        {config.useMock && (
          <p className="login-hint">
            Demo: <code>admin@uptc.edu.co</code> / <code>Admin2026*</code>
          </p>
        )}
        <Link to="/" className="login-back">
          ← Volver al chat
        </Link>
      </form>
    </div>
  );
}
