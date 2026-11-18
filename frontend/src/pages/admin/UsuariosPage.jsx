import { useCallback, useEffect, useState } from 'react';
import Drawer from '../../components/admin/Drawer.jsx';
import Icon from '../../components/Icon.jsx';
import { useAuth } from '../../hooks/useAuth.jsx';
import { adminService } from '../../services/adminService.js';
import { formatDateTime } from '../../utils/format.js';

const ROLES = [
  { id: 'administrador', texto: 'Administrador (gestor de conocimiento)' },
  { id: 'superadministrador', texto: 'Superadministrador' },
];

function ClaveTemporal({ correo, clave, onCerrar }) {
  const [copiada, setCopiada] = useState(false);
  return (
    <div className="notice notice-warning secreto" role="status">
      <div>
        <strong>Contraseña temporal de {correo}</strong>
        <p>Compártela por un canal seguro. Solo se muestra esta vez; la persona debe cambiarla al entrar.</p>
        <code className="secreto-valor">{clave}</code>
      </div>
      <div className="secreto-acciones">
        <button
          type="button"
          className="btn btn-dark btn-sm"
          onClick={() => {
            navigator.clipboard?.writeText(clave);
            setCopiada(true);
          }}
        >
          {copiada ? 'Copiada' : 'Copiar'}
        </button>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onCerrar}>
          Listo
        </button>
      </div>
    </div>
  );
}

function NuevoUsuario({ onCreado }) {
  const { manejarError } = useAuth();
  const [form, setForm] = useState({ nombre: '', correo: '', rol: 'administrador' });
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const set = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));
  const err = (campo) => (error?.campo === campo ? error.mensaje : null);

  const enviar = async (e) => {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    try {
      onCreado(await adminService.crearUsuario(form));
    } catch (x) {
      manejarError(x);
      setError({ campo: x.detalles?.campo, mensaje: x.message });
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form className="editor" onSubmit={enviar} noValidate>
      {error && !error.campo && <p className="notice notice-critical">{error.mensaje}</p>}
      <div className="field">
        <label htmlFor="nu-nombre">Nombre completo *</label>
        <input id="nu-nombre" className="input" value={form.nombre} onChange={set('nombre')} aria-invalid={!!err('nombre')} />
        {err('nombre') && <span className="field-error">{err('nombre')}</span>}
      </div>
      <div className="field">
        <label htmlFor="nu-correo">Correo institucional *</label>
        <input
          id="nu-correo"
          className="input"
          type="email"
          placeholder="nombre.apellido@uptc.edu.co"
          value={form.correo}
          onChange={set('correo')}
          aria-invalid={!!err('correo')}
        />
        {err('correo') ? (
          <span className="field-error">{err('correo')}</span>
        ) : (
          <span className="field-hint">Solo se aceptan cuentas @uptc.edu.co (UC-14).</span>
        )}
      </div>
      <div className="field">
        <label htmlFor="nu-rol">Rol</label>
        <select id="nu-rol" className="select" value={form.rol} onChange={set('rol')}>
          {ROLES.map((r) => (
            <option key={r.id} value={r.id}>
              {r.texto}
            </option>
          ))}
        </select>
      </div>
      <div>
        <button type="submit" className="btn btn-primary" disabled={guardando}>
          {guardando ? 'Creando…' : 'Crear usuario'}
        </button>
      </div>
    </form>
  );
}

// UserManager (UC-14): solo el superadministrador. Las cuentas se desactivan, nunca se borran.
export default function UsuariosPage() {
  const { usuario, manejarError } = useAuth();
  const [lista, setLista] = useState(null);
  const [creando, setCreando] = useState(false);
  const [clave, setClave] = useState(null);
  const [error, setError] = useState('');
  const [ocupado, setOcupado] = useState(null);

  const cargar = useCallback(() => {
    adminService
      .listarUsuarios()
      .then(setLista)
      .catch((e) => {
        manejarError(e);
        setError(e.codigo === 'PROHIBIDO' ? 'Solo un superadministrador puede gestionar usuarios.' : 'No se pudo cargar la lista.');
      });
  }, [manejarError]);

  useEffect(cargar, [cargar]);

  const cambiar = async (u, cambios) => {
    setOcupado(u.id);
    setError('');
    try {
      await adminService.actualizarUsuario(u.id, cambios);
      cargar();
    } catch (e) {
      manejarError(e);
      setError(e.message);
    } finally {
      setOcupado(null);
    }
  };

  const restablecer = async (u) => {
    if (!window.confirm(`¿Generar una contraseña temporal nueva para ${u.correo}?`)) return;
    setOcupado(u.id);
    try {
      const r = await adminService.restablecerContrasena(u.id);
      setClave({ correo: u.correo, clave: r.contrasenaTemporal });
    } catch (e) {
      manejarError(e);
      setError(e.message);
    } finally {
      setOcupado(null);
    }
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Usuarios del panel</h1>
          <p className="page-sub">Quién puede entrar al panel y con qué permisos</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setCreando(true)}>
          <Icon name="plus" size={18} /> Nuevo usuario
        </button>
      </div>

      {clave && <ClaveTemporal {...clave} onCerrar={() => setClave(null)} />}
      {error && <p className="notice notice-critical">{error}</p>}

      <div className={`card card-flush ${!lista ? 'is-loading' : ''}`}>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Usuario</th>
                <th scope="col">Rol</th>
                <th scope="col">Estado</th>
                <th scope="col">Último acceso</th>
                <th scope="col">
                  <span className="sr-only">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {lista?.map((u) => {
                const yo = String(u.id) === String(usuario?.id);
                return (
                  <tr key={u.id}>
                    <td>
                      {u.nombre} {yo && <span className="badge badge-plain">Tú</span>}
                      <span className="cell-sub">{u.correo}</span>
                    </td>
                    <td>
                      <label htmlFor={`rol-${u.id}`} className="sr-only">
                        Rol de {u.nombre}
                      </label>
                      <select
                        id={`rol-${u.id}`}
                        className="select select-auto"
                        value={u.rol}
                        disabled={yo || ocupado === u.id}
                        onChange={(e) => cambiar(u, { rol: e.target.value })}
                      >
                        {ROLES.map((r) => (
                          <option key={r.id} value={r.id}>
                            {r.id}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <span className={`badge ${u.activo ? 'badge-good' : 'badge-neutral'}`}>
                        {u.activo ? 'Activo' : 'Desactivado'}
                      </span>
                      {u.debeCambiarPassword && <span className="cell-sub">Debe cambiar la contraseña</span>}
                    </td>
                    <td className="cell-muted">{u.ultimoAcceso ? formatDateTime(u.ultimoAcceso) : 'Nunca'}</td>
                    <td className="cell-actions">
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        disabled={ocupado === u.id}
                        onClick={() => restablecer(u)}
                      >
                        <Icon name="refresh" size={15} /> Contraseña
                      </button>
                      {!yo && (
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm"
                          disabled={ocupado === u.id}
                          onClick={() => cambiar(u, { activo: !u.activo })}
                        >
                          {u.activo ? 'Desactivar' : 'Activar'}
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {creando && (
        <Drawer titulo="Nuevo usuario" onClose={() => setCreando(false)}>
          <NuevoUsuario
            onCreado={(r) => {
              setCreando(false);
              setClave({ correo: r.usuario.correo, clave: r.contrasenaTemporal });
              cargar();
            }}
          />
        </Drawer>
      )}
    </div>
  );
}
