# Asistente Virtual UPTC — Frontend

Frontend del proyecto **"Implementación de un Asistente Virtual Automatizado para la Optimización del Soporte Informativo y la Gestión Administrativa en la UPTC"** (Ingeniería del Software II, 2026).

Incluye:

- **Widget de chat del estudiante** (`/`): botón flotante «¿Necesitas ayuda?», temas rápidos, contador de 500 caracteres, indicador de escritura, escalamiento a atención humana cuando la confianza es menor al 60 %, aviso sin conexión y pantalla completa en móvil.
- **Panel administrativo** (`/admin`): inicio de sesión con JWT, dashboard de métricas, base de conocimiento (crear intenciones con ejemplos, editar, activar o desactivar respuestas, historial de cambios), historial de consultas con exportación a CSV, usuarios y configuración del sistema (solo superadministrador) y cambio de contraseña.

> Este es el frontend del proyecto. La guía general, el backend y el servicio PLN están en el [README principal](../README.md).

Stack: **React 18 + Vite 7** (sin librerías de UI ni de gráficas). Pruebas con **Vitest** (`npm test`).

## Cómo ejecutarlo

Requisito: **Node.js 20.19 o superior** (`node -v` para comprobarlo).

```bash
npm install
npm run dev
```

Abre <http://localhost:5173>.

| Ruta | Qué hay |
| --- | --- |
| `/` | Página de demostración con el widget de chat |
| `/admin` | Panel administrativo |

**Usuario de demostración del panel:** `admin@uptc.edu.co` / `Admin2026*`

Para generar la versión de producción: `npm run build` (queda en `dist/`).

## Modo demostración vs. backend real

Por defecto el proyecto funciona **sin backend**: un motor PLN simulado clasifica las preguntas por palabras clave y los datos se guardan en el `localStorage` del navegador. El chat y el panel comparten esos datos: si desactivas una respuesta en el panel, el chat empieza a escalar esa consulta.

Para conectarlo al backend Spring Boot, copia `.env.example` como `.env` y cambia:

```
VITE_USE_MOCK=false
VITE_BACKEND_URL=http://localhost:8080
```

En desarrollo Vite redirige `/api/*` al backend (ver `vite.config.js`).

> Los textos, correos y enlaces del modo demostración son de ejemplo. Antes del piloto el enlace institucional debe reemplazarlos por los oficiales (RD-01, RD-03, RD-04).

## Contrato de la API REST esperado

| Método | Ruta | Cuerpo / respuesta |
| --- | --- | --- |
| POST | `/api/chat/sesiones` | `{ canal }` → `{ sesionId }` |
| POST | `/api/chat/consultas` | `{ sesionId, texto }` → `{ tipo: "respuesta" \| "escalamiento", texto, intent, confianza, dependencia }` |
| POST | `/api/chat/sesiones/{id}/cerrar` | — |
| GET | `/api/chat/temas` | `[{ id, etiqueta, consulta }]` |
| POST | `/api/admin/auth/login` | `{ correo, password }` → `{ token, usuario }` |
| GET | `/api/admin/metricas?rango=7d` | totales, tasa de resolución, serie por día, top de intenciones |
| GET | `/api/admin/intenciones?q=&categoria=&page=` | lista paginada |
| GET / PUT | `/api/admin/intenciones/{id}` | detalle / editar |
| POST | `/api/admin/intenciones` | crear intención con su primera respuesta |
| POST | `/api/admin/intenciones/{id}/respuestas` | `{ texto }` |
| PATCH | `/api/admin/intenciones/{id}/respuestas/{rid}` | `{ texto?, activa? }` |
| GET | `/api/admin/intenciones/{id}/historial` | eventos de la intención (EventSourcing) |
| GET | `/api/admin/dependencias` | dependencias y datos de contacto |
| GET | `/api/admin/consultas?rango=&estado=&q=&page=` | historial paginado de consultas |
| GET | `/api/admin/consultas/exportar?rango=` | CSV del historial |
| PUT | `/api/admin/auth/contrasena` | `{ actual, nueva }` |
| GET / POST / PATCH | `/api/admin/usuarios[/{id}]` | cuentas del panel (solo superadministrador) |
| POST | `/api/admin/usuarios/{id}/restablecer` | → `{ contrasenaTemporal }` |
| GET / PUT | `/api/admin/configuracion` | umbral (50–80 %), inactividad, plantilla, notificaciones |
| PUT | `/api/admin/dependencias/{clave}` | datos de contacto de una dependencia |
| GET | `/api/admin/auditoria?limite=` | últimas acciones del panel |
| GET / POST | `/api/admin/pln/estado`, `/api/admin/pln/reentrenar` | estado y reentrenamiento del motor PLN |

La documentación completa e interactiva está en `/swagger-ui.html` del backend.

Las rutas `/api/admin/*` llevan `Authorization: Bearer <JWT>`. Un `401` cierra la sesión del panel; un `503` en el chat muestra el mensaje de servicio no disponible con el contacto de atención.

## Estructura

```
src/
├── components/
│   ├── chat/      ChatWidget, MessageList, InputBox, TypingIndicator, QuickTopics, EscalationCard
│   ├── admin/     AdminLayout, Drawer
│   └── charts/    StatTile, ColumnChart, BarList
├── pages/
│   ├── PortalDemoPage.jsx
│   └── admin/     LoginPage, DashboardPage, KnowledgeBasePage, IntentEditor, NewIntentForm,
│                  HistorialPage, UsuariosPage, ConfiguracionPage, CuentaPage
├── hooks/         useChat (sesión y conversación), useAuth (JWT), useTheme, useOnlineStatus
├── services/
│   ├── chatService.js / adminService.js   eligen simulador o API real
│   ├── chatApi.js / adminApi.js            llamadas REST reales
│   └── mock/                               motor PLN simulado y datos de ejemplo
└── styles/        tokens de diseño (modo claro y oscuro)
```

## Relación con los requisitos

| Requisito | Dónde se cumple en el frontend |
| --- | --- |
| RF-01 Consulta en lenguaje natural (≤ 500) | `InputBox.jsx`: contador, bloqueo de envío vacío o excedido, indicador inmediato |
| RF-03 Respuesta automática | `useChat.js` → `chatService.enviarConsulta` |
| RF-05 Escalamiento < 60 % | `EscalationCard.jsx`: dependencia, correo, enlace y «Enviar mi consulta por correo» |
| RF-07 Panel de administración | `KnowledgeBasePage.jsx`, `IntentEditor.jsx` (cambios visibles en el chat al instante) |
| RF-08 Multiplataforma (360–1920 px) | `chat.css` y `admin.css`: pantalla completa en móvil |
| RNF-08 Fiabilidad | Aviso sin conexión, reintento ante errores de red y tiempo de espera |
| RD-02 Privacidad | Sesiones anónimas (`sesionId`), aviso de Ley 1581 en el saludo |
| UC-09 Fin de sesión | Botón «Finalizar conversación» y cierre automático tras 30 min de inactividad |
