import { Link, NavLink, Outlet } from 'react-router-dom';
import { config } from '../../config.js';
import { useAuth } from '../../hooks/useAuth.jsx';
import { useTheme } from '../../hooks/useTheme.jsx';
import Icon from '../Icon.jsx';
import './admin.css';

const NAV = [
  { to: '/admin', end: true, icono: 'dashboard', texto: 'Dashboard' },
  { to: '/admin/conocimiento', icono: 'book', texto: 'Base de conocimiento' },
];

export default function AdminLayout() {
  const { usuario, logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const oscuro =
    theme === 'dark' || (theme === 'system' && window.matchMedia?.('(prefers-color-scheme: dark)').matches);

  return (
    <div className="adm">
      <aside className="adm-side">
        <Link to="/admin" className="adm-brand">
          <span className="adm-mark" aria-hidden="true">
            <Icon name="chat" size={18} />
          </span>
          <span>
            <strong>Asistente UPTC</strong>
            <small>Panel administrativo</small>
          </span>
        </Link>

        <nav className="adm-nav" aria-label="Secciones del panel">
          {NAV.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className="adm-nav-link">
              <Icon name={n.icono} size={18} />
              <span>{n.texto}</span>
            </NavLink>
          ))}
          <Link to="/" className="adm-nav-link">
            <Icon name="external" size={18} />
            <span>Ver el chat</span>
          </Link>
        </nav>

        <div className="adm-user">
          <div className="adm-user-info">
            <strong>{usuario?.nombre}</strong>
            <small>{usuario?.correo}</small>
          </div>
          <div className="adm-user-actions">
            <button
              type="button"
              className="btn btn-ghost btn-icon"
              onClick={() => setTheme(oscuro ? 'light' : 'dark')}
              aria-label={oscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
              title={oscuro ? 'Modo claro' : 'Modo oscuro'}
            >
              <Icon name={oscuro ? 'sun' : 'moon'} size={18} />
            </button>
            <button type="button" className="btn btn-ghost btn-icon" onClick={() => logout()} aria-label="Cerrar sesión" title="Cerrar sesión">
              <Icon name="logout" size={18} />
            </button>
          </div>
        </div>
      </aside>

      <main className="adm-main">
        {config.useMock && (
          <p className="adm-demo">
            <Icon name="alert" size={15} /> Modo demostración: datos simulados guardados en este navegador.
          </p>
        )}
        <Outlet />
      </main>
    </div>
  );
}
