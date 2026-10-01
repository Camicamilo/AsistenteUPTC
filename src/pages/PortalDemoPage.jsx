import { Link } from 'react-router-dom';
import ChatWidget from '../components/chat/ChatWidget.jsx';
import Icon from '../components/Icon.jsx';
import { modoDemo } from '../services/chatService.js';
import './portal.css';

const EJEMPLOS = [
  { texto: '¿Cuándo son las matrículas?', nota: 'Respuesta automática' },
  { texto: 'Necesito el plástico ese de estudiante', nota: 'Lenguaje informal → carné estudiantil' },
  { texto: 'Olvidé la contraseña del correo', nota: 'Soporte técnico' },
  { texto: '¿Quién ganó el partido de ayer?', nota: 'Fuera del dominio → escalamiento' },
];

const CARACTERISTICAS = [
  { icono: 'clock', titulo: 'Disponible 24/7', texto: 'Atiende fuera del horario de oficina, también de noche y los fines de semana.' },
  { icono: 'sparkles', titulo: 'Entiende lenguaje natural', texto: 'Escribe como hablas: identifica la intención aunque la pregunta sea informal.' },
  { icono: 'users', titulo: 'Escala a una persona', texto: 'Si no está seguro (confianza < 60 %), te indica la dependencia y cómo contactarla.' },
  { icono: 'shield', titulo: 'Conversaciones anónimas', texto: 'No pide nombre, documento ni código estudiantil (Ley 1581 de 2012).' },
];

const probar = (texto) => window.dispatchEvent(new CustomEvent('av:consulta', { detail: texto }));

export default function PortalDemoPage() {
  return (
    <div className="portal">
      <header className="portal-top">
        <div className="portal-brand">
          <span className="portal-mark" aria-hidden="true">
            <Icon name="chat" size={20} />
          </span>
          <span>
            <strong>Asistente Virtual UPTC</strong>
            <small>Página de demostración</small>
          </span>
        </div>
        <Link to="/admin" className="btn btn-ghost btn-sm">
          Panel administrativo
        </Link>
      </header>

      <main>
        <section className="portal-hero">
          <p className="portal-eyebrow">Ingeniería del Software II · Prototipo funcional</p>
          <h1>Resuelve tus dudas académicas y administrativas a cualquier hora</h1>
          <p className="portal-lead">
            El Asistente Virtual es la primera capa de atención para admisiones, matrículas, calendario académico, correo
            institucional y trámites. Responde en segundos y, si no puede resolver algo, te dice a dónde acudir.
          </p>
          <div className="portal-cta">
            <button type="button" className="btn btn-primary" onClick={() => window.dispatchEvent(new CustomEvent('av:abrir'))}>
              <Icon name="chat" size={18} /> Abrir el asistente
            </button>
            <Link to="/admin" className="btn btn-ghost">
              Ir al panel administrativo
            </Link>
          </div>
          {modoDemo && (
            <p className="portal-note">
              <Icon name="alert" size={15} /> Modo demostración: las respuestas son simuladas y no constituyen información
              oficial de la UPTC.
            </p>
          )}
        </section>

        <section className="portal-section" aria-labelledby="probar-titulo">
          <h2 id="probar-titulo">Prueba estas preguntas</h2>
          <div className="portal-examples">
            {EJEMPLOS.map((e) => (
              <button key={e.texto} type="button" className="portal-example" onClick={() => probar(e.texto)}>
                <span className="portal-example-q">«{e.texto}»</span>
                <span className="portal-example-n">{e.nota}</span>
              </button>
            ))}
          </div>
        </section>

        <section className="portal-section" aria-labelledby="carac-titulo">
          <h2 id="carac-titulo">Cómo funciona</h2>
          <div className="portal-features">
            {CARACTERISTICAS.map((c) => (
              <article key={c.titulo} className="portal-feature">
                <span className="portal-feature-icon" aria-hidden="true">
                  <Icon name={c.icono} size={20} />
                </span>
                <h3>{c.titulo}</h3>
                <p>{c.texto}</p>
              </article>
            ))}
          </div>
        </section>
      </main>

      <footer className="portal-footer">
        <p>
          Adriam Camilo Macias Gavidia · Sergio Mauricio Parra Criado — Ingeniería de Sistemas y Computación, UPTC Sogamoso,
          2026
        </p>
        <p>En producción, el widget se integra en el portal institucional con un script embebido.</p>
      </footer>

      <ChatWidget />
    </div>
  );
}
