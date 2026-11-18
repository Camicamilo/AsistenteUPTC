import { useCallback, useEffect, useState } from 'react';
import Drawer from '../../components/admin/Drawer.jsx';
import Icon from '../../components/Icon.jsx';
import { REGLAS } from '../../config.js';
import { useAuth } from '../../hooks/useAuth.jsx';
import { adminService } from '../../services/adminService.js';
import { formatDateTime } from '../../utils/format.js';

function EditarDependencia({ dependencia, onGuardada }) {
  const { manejarError } = useAuth();
  const [form, setForm] = useState({
    nombre: dependencia.nombre || '',
    correo: dependencia.correo || '',
    telefono: dependencia.telefono || '',
    enlace: dependencia.enlace || '',
    horario: dependencia.horario || '',
  });
  const [error, setError] = useState(null);
  const set = (c) => (e) => setForm((f) => ({ ...f, [c]: e.target.value }));
  const err = (c) => (error?.campo === c ? error.mensaje : null);

  const guardar = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      onGuardada(await adminService.actualizarDependencia(dependencia.clave, form));
    } catch (x) {
      manejarError(x);
      setError({ campo: x.detalles?.campo, mensaje: x.message });
    }
  };

  const campo = (id, etiqueta, tipo = 'text', ayuda = '') => (
    <div className="field">
      <label htmlFor={`dep-${id}`}>{etiqueta}</label>
      <input id={`dep-${id}`} className="input" type={tipo} value={form[id]} onChange={set(id)} aria-invalid={!!err(id)} />
      {err(id) ? <span className="field-error">{err(id)}</span> : ayuda && <span className="field-hint">{ayuda}</span>}
    </div>
  );

  return (
    <form className="editor" onSubmit={guardar} noValidate>
      {error && !error.campo && <p className="notice notice-critical">{error.mensaje}</p>}
      {campo('nombre', 'Nombre oficial *', 'text', 'Usa la terminología institucional (RD-04).')}
      {campo('correo', 'Correo', 'email')}
      {campo('telefono', 'Teléfono')}
      {campo('enlace', 'Enlace', 'url', 'Debe empezar por https://')}
      {campo('horario', 'Horario de atención')}
      <div>
        <button type="submit" className="btn btn-primary">
          Guardar
        </button>
      </div>
    </form>
  );
}

// Configuración del sistema (UC-16). Los cambios aplican de inmediato, sin reiniciar.
export default function ConfiguracionPage() {
  const { manejarError } = useAuth();
  const [cfg, setCfg] = useState(null);
  const [original, setOriginal] = useState(null);
  const [deps, setDeps] = useState([]);
  const [editandoDep, setEditandoDep] = useState(null);
  const [auditoria, setAuditoria] = useState([]);
  const [pln, setPln] = useState(null);
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState(null);
  const [ocupado, setOcupado] = useState(false);

  const cargar = useCallback(() => {
    adminService
      .configuracion()
      .then((c) => {
        setCfg(c);
        setOriginal(c);
      })
      .catch(manejarError);
    adminService.dependencias().then(setDeps).catch(manejarError);
    adminService.auditoria(50).then(setAuditoria).catch(() => setAuditoria([]));
    adminService.estadoPln().then(setPln).catch(() => setPln({ disponible: false, circuito: 'desconocido' }));
  }, [manejarError]);

  useEffect(cargar, [cargar]);

  const avisar = (t) => {
    setAviso(t);
    setTimeout(() => setAviso(''), 3500);
  };

  const guardar = async (accion) => {
    setOcupado(true);
    setError(null);
    try {
      const c = accion === 'restaurar' ? await adminService.restaurarConfiguracion() : await adminService.guardarConfiguracion(cfg);
      setCfg(c);
      setOriginal(c);
      avisar(accion === 'restaurar' ? 'Se restauraron los valores por defecto.' : 'Configuración guardada. Ya está aplicando en el chat.');
      adminService.auditoria(50).then(setAuditoria).catch(() => {});
    } catch (x) {
      manejarError(x);
      setError({ campo: x.detalles?.campo, mensaje: x.message });
    } finally {
      setOcupado(false);
    }
  };

  const reentrenar = async () => {
    setOcupado(true);
    try {
      await adminService.reentrenarPln();
      avisar('El motor PLN se reentrenó con la base de conocimiento actual.');
      adminService.estadoPln().then(setPln).catch(() => {});
    } catch (x) {
      manejarError(x);
      setError({ mensaje: 'No se pudo reentrenar el motor PLN: ' + x.message });
    } finally {
      setOcupado(false);
    }
  };

  if (!cfg) return <p className="empty">Cargando configuración…</p>;

  const cambiado = JSON.stringify(cfg) !== JSON.stringify(original);
  const err = (c) => (error?.campo === c ? error.mensaje : null);
  const ejemploDep = deps.find((d) => d.clave === 'registro')?.nombre || 'Registro y Control Académico';

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Configuración</h1>
          <p className="page-sub">Parámetros del asistente, contactos de las dependencias y auditoría</p>
        </div>
      </div>

      {aviso && (
        <p className="notice notice-good" role="status">
          <Icon name="check" size={16} /> {aviso}
        </p>
      )}
      {error && !error.campo && <p className="notice notice-critical">{error.mensaje}</p>}

      <section className="card">
        <h2 className="card-title">Parámetros del asistente</h2>
        <div className="grid-2">
          <div className="field">
            <label htmlFor="cfg-umbral">
              Umbral de confianza: <strong>{Math.round(cfg.umbralConfianza * 100)} %</strong>
            </label>
            <input
              id="cfg-umbral"
              type="range"
              min={REGLAS.UMBRAL_MIN * 100}
              max={REGLAS.UMBRAL_MAX * 100}
              step={1}
              value={Math.round(cfg.umbralConfianza * 100)}
              onChange={(e) => setCfg((c) => ({ ...c, umbralConfianza: Number(e.target.value) / 100 }))}
            />
            {err('umbralConfianza') ? (
              <span className="field-error">{err('umbralConfianza')}</span>
            ) : (
              <span className="field-hint">
                Por debajo de este valor la consulta se escala a una persona. Rango permitido: 50–80 % (UC-16).
              </span>
            )}
          </div>
          <div className="field">
            <label htmlFor="cfg-inact">Cierre de sesión por inactividad (minutos)</label>
            <input
              id="cfg-inact"
              className="input"
              type="number"
              min={5}
              max={240}
              value={cfg.inactividadSesionMin}
              onChange={(e) => setCfg((c) => ({ ...c, inactividadSesionMin: Number(e.target.value) }))}
              aria-invalid={!!err('inactividadSesionMin')}
            />
            {err('inactividadSesionMin') && <span className="field-error">{err('inactividadSesionMin')}</span>}
          </div>
        </div>
        <div className="field">
          <label htmlFor="cfg-plantilla">Mensaje de escalamiento</label>
          <textarea
            id="cfg-plantilla"
            className="textarea"
            value={cfg.plantillaEscalamiento}
            onChange={(e) => setCfg((c) => ({ ...c, plantillaEscalamiento: e.target.value }))}
            aria-invalid={!!err('plantillaEscalamiento')}
          />
          {err('plantillaEscalamiento') ? (
            <span className="field-error">{err('plantillaEscalamiento')}</span>
          ) : (
            <span className="field-hint">
              Vista previa: «{cfg.plantillaEscalamiento.replace('{dependencia}', ejemploDep)}»
            </span>
          )}
        </div>
        <label className="check">
          <input
            type="checkbox"
            checked={cfg.notificarPorCorreo}
            onChange={(e) => setCfg((c) => ({ ...c, notificarPorCorreo: e.target.checked }))}
          />
          Avisar por correo al personal cuando una consulta se escale (UC-11)
        </label>
        <div className="row-actions">
          <button type="button" className="btn btn-primary" disabled={!cambiado || ocupado} onClick={() => guardar('guardar')}>
            Guardar cambios
          </button>
          <button
            type="button"
            className="btn btn-ghost"
            disabled={ocupado}
            onClick={() => window.confirm('¿Restaurar los valores por defecto?') && guardar('restaurar')}
          >
            Restaurar valores por defecto
          </button>
        </div>
      </section>

      <section className="card">
        <h2 className="card-title">Motor de lenguaje natural</h2>
        <p className="kv">
          Estado:{' '}
          {pln == null ? (
            '…'
          ) : pln.disponible ? (
            <span className="badge badge-good">Disponible</span>
          ) : (
            <span className="badge badge-critical">No responde</span>
          )}{' '}
          · Circuit breaker: <code>{pln?.circuito ?? '…'}</code>
        </p>
        <p className="field-hint">
          Al crear una intención el modelo se reentrena solo. Usa este botón si cambiaste ejemplos directamente en la base de
          datos o si el motor se reinició.
        </p>
        <div className="row-actions">
          <button type="button" className="btn btn-dark" disabled={ocupado} onClick={reentrenar}>
            <Icon name="refresh" size={16} /> Reentrenar ahora
          </button>
        </div>
      </section>

      <section className="card card-flush">
        <h2 className="card-title card-title-pad">Dependencias (datos de contacto del escalamiento)</h2>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Dependencia</th>
                <th scope="col">Correo</th>
                <th scope="col">Horario</th>
                <th scope="col">
                  <span className="sr-only">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {deps.map((d) => (
                <tr key={d.clave}>
                  <td>
                    {d.nombre}
                    <span className="cell-sub">{d.clave}</span>
                  </td>
                  <td className="cell-muted">{d.correo || '—'}</td>
                  <td className="cell-muted">{d.horario || '—'}</td>
                  <td className="cell-actions">
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditandoDep(d)}>
                      <Icon name="edit" size={15} /> Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card card-flush">
        <h2 className="card-title card-title-pad">Auditoría (últimas 50 acciones)</h2>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Usuario</th>
                <th scope="col">Acción</th>
                <th scope="col">Detalle</th>
              </tr>
            </thead>
            <tbody>
              {auditoria.map((a) => (
                <tr key={a.id}>
                  <td className="cell-muted">{formatDateTime(a.fecha)}</td>
                  <td>{a.usuario}</td>
                  <td>{a.accion}</td>
                  <td className="cell-muted">{a.detalle}</td>
                </tr>
              ))}
              {auditoria.length === 0 && (
                <tr>
                  <td colSpan={4} className="empty">
                    Sin registros todavía.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      {editandoDep && (
        <Drawer titulo={`Editar ${editandoDep.nombre}`} onClose={() => setEditandoDep(null)}>
          <EditarDependencia
            dependencia={editandoDep}
            onGuardada={() => {
              setEditandoDep(null);
              avisar('Datos de contacto actualizados.');
              cargar();
            }}
          />
        </Drawer>
      )}
    </div>
  );
}
