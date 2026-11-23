import { useEffect, useId, useRef, useState } from 'react';
import { useChat } from '../../hooks/useChat.js';
import { useOnlineStatus } from '../../hooks/useOnlineStatus.js';
import { modoDemo } from '../../services/chatService.js';
import Icon from '../Icon.jsx';
import InputBox from './InputBox.jsx';
import MessageList from './MessageList.jsx';
import QuickTopics from './QuickTopics.jsx';
import './chat.css';

/**
 * ChatWidget: botón flotante «¿Necesitas ayuda?» + ventana de chat (prototipo 6.2).
 * En escritorio es una ventana flotante; en móvil ocupa toda la pantalla (RF-08).
 */
export default function ChatWidget() {
  const [abierto, setAbierto] = useState(false);
  const [texto, setTexto] = useState('');
  const [temasAbiertos, setTemasAbiertos] = useState(false);
  const [confirmarCierre, setConfirmarCierre] = useState(false);
  const online = useOnlineStatus();
  const chat = useChat();
  const inputRef = useRef(null);
  const launcherRef = useRef(null);
  const tituloId = useId();
  const panelId = useId();
  const consultaPendiente = useRef(null);

  // La página anfitriona puede abrir el chat o enviar una consulta de ejemplo:
  // window.dispatchEvent(new CustomEvent('av:abrir'))
  // window.dispatchEvent(new CustomEvent('av:consulta', { detail: '¿Cuándo son las matrículas?' }))
  useEffect(() => {
    const abrir = () => setAbierto(true);
    const consulta = (e) => {
      consultaPendiente.current = String(e.detail || '');
      setAbierto(true);
    };
    window.addEventListener('av:abrir', abrir);
    window.addEventListener('av:consulta', consulta);
    return () => {
      window.removeEventListener('av:abrir', abrir);
      window.removeEventListener('av:consulta', consulta);
    };
  }, []);

  // UC-04: al abrir el widget se crea (o reutiliza) la sesión anónima.
  useEffect(() => {
    if (abierto && !chat.sesionId) chat.iniciar();
  }, [abierto, chat.sesionId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (abierto) setTimeout(() => inputRef.current?.focus(), 60);
  }, [abierto]);

  useEffect(() => {
    if (abierto && chat.sesionId && !chat.iniciando && !chat.escribiendo && consultaPendiente.current) {
      const q = consultaPendiente.current;
      consultaPendiente.current = null;
      enviar(q);
    }
  }); // eslint-disable-line react-hooks/exhaustive-deps

  // Al volver la conexión se reactiva la caja de texto automáticamente (RNF-08).
  useEffect(() => {
    if (online && abierto) inputRef.current?.focus();
  }, [online]); // eslint-disable-line react-hooks/exhaustive-deps

  const minimizar = () => {
    setAbierto(false);
    setConfirmarCierre(false);
    setTimeout(() => launcherRef.current?.focus(), 0);
  };

  const finalizar = () => {
    chat.finalizar();
    setTexto('');
    setTemasAbiertos(false);
    minimizar();
  };

  const enviar = (valor) => {
    if (!online) return;
    if (chat.enviar(valor)) {
      setTexto('');
      setTemasAbiertos(false);
    }
  };

  const reformular = (consulta) => {
    setTexto(consulta || '');
    setTimeout(() => {
      const el = inputRef.current;
      if (el) {
        el.focus();
        el.setSelectionRange(el.value.length, el.value.length);
      }
    }, 0);
  };

  return (
    <div className="av-root">
      {abierto && (
        <section
          id={panelId}
          className="av-window"
          role="dialog"
          aria-modal="false"
          aria-labelledby={tituloId}
          onKeyDown={(e) => {
            if (e.key === 'Escape') minimizar();
          }}
        >
          <header className="av-header">
            <div className="av-header-avatar" aria-hidden="true">
              <Icon name="chat" size={20} />
            </div>
            <div className="av-header-text">
              <h2 id={tituloId}>Asistente Virtual UPTC</h2>
              <p className={`av-status ${online ? 'is-online' : 'is-offline'}`}>
                {online ? 'En línea' : 'Sin conexión'}
                {modoDemo && <span className="av-demo">Demo</span>}
              </p>
            </div>
            <button type="button" className="av-header-btn" onClick={minimizar} aria-label="Minimizar chat" title="Minimizar">
              <Icon name="minus" size={18} />
            </button>
            <button
              type="button"
              className="av-header-btn"
              onClick={() => (chat.mensajes.length > 1 ? setConfirmarCierre(true) : finalizar())}
              aria-label="Finalizar conversación"
              title="Finalizar conversación"
            >
              <Icon name="close" size={18} />
            </button>
          </header>

          {confirmarCierre && (
            <div className="av-confirm" role="alertdialog" aria-label="Confirmar cierre">
              <p>¿Finalizar la conversación? Se borrará el historial de este chat.</p>
              <div>
                <button type="button" className="av-btn av-btn-dark" onClick={finalizar}>
                  Finalizar
                </button>
                <button type="button" className="av-btn av-btn-ghost" onClick={() => setConfirmarCierre(false)}>
                  Cancelar
                </button>
              </div>
            </div>
          )}

          {!online && (
            <div className="av-offline" role="status">
              <Icon name="wifiOff" size={16} />
              Perdiste la conexión. Podrás escribir de nuevo en cuanto vuelva.
            </div>
          )}

          <MessageList
            mensajes={chat.mensajes}
            escribiendo={chat.escribiendo || chat.iniciando}
            temas={chat.temas}
            onTopic={enviar}
            onRetry={chat.reintentar}
            onReformular={reformular}
          />

          {temasAbiertos && (
            <div id="av-temas-bandeja" className="av-topics-tray">
              <p>Temas frecuentes</p>
              <QuickTopics temas={chat.temas} onSelect={enviar} disabled={chat.escribiendo || !online} />
            </div>
          )}

          <InputBox
            value={texto}
            onChange={(v) => {
              setTexto(v);
              chat.tocar();
            }}
            onSend={enviar}
            ocupado={chat.escribiendo}
            sinConexion={!online}
            inputRef={inputRef}
            temasAbiertos={temasAbiertos}
            onToggleTemas={() => setTemasAbiertos((v) => !v)}
          />
        </section>
      )}

      <button
        ref={launcherRef}
        type="button"
        className={`av-launcher ${abierto ? 'is-open' : ''}`}
        onClick={() => (abierto ? minimizar() : setAbierto(true))}
        aria-expanded={abierto}
        aria-controls={abierto ? panelId : undefined}
        aria-label={abierto ? 'Minimizar chat' : '¿Necesitas ayuda? Abrir el Asistente Virtual'}
      >
        <Icon name={abierto ? 'minus' : 'chat'} size={22} />
        {!abierto && (
          <span className="av-launcher-label" aria-hidden="true">
            ¿Necesitas ayuda?
          </span>
        )}
      </button>
    </div>
  );
}
