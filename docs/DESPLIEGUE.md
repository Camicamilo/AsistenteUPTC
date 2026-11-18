# Despliegue del Asistente Virtual UPTC

Hay dos formas de montarlo. La primera sirve para la sustentación en un computador; la segunda deja la
aplicación publicada en internet sin costo (actividad complementaria «Aplicación debe estar montada»).

## 1. En un computador con Docker (todo junto)

Requisito: Docker Desktop (Windows/Mac) o Docker Engine (Linux).

```bash
git clone https://github.com/Camicamilo/AsistenteUPTC.git
cd AsistenteUPTC
cp .env.example .env        # cambia DB_CLAVE y JWT_SECRETO
docker compose up -d --build
```

La primera vez tarda unos minutos (descarga imágenes y compila). Luego:

| Dirección | Qué es |
| --- | --- |
| http://localhost | Portal de demostración con el chat |
| http://localhost/admin | Panel administrativo (`admin@uptc.edu.co` / `Admin2026*`) |
| http://localhost/swagger-ui.html | Documentación interactiva de la API |

Comandos útiles: `docker compose ps` (estado), `docker compose logs -f backend` (logs),
`docker compose down` (apagar; los datos quedan en el volumen), `docker compose down -v` (borrar todo).

Para generar tráfico real y probar la carga (RNF-05):

```bash
python scripts/prueba_carga.py http://localhost 100 5
python scripts/prueba_e2e.py http://localhost
```

## 2. En internet, gratis (Render + Neon)

Los planes gratuitos cambian; antes de desplegar revisa los límites vigentes en
<https://render.com/docs/free> y <https://neon.com/pricing>.

**Base de datos — Neon.** Su plan gratis no vence (el PostgreSQL gratis de Render se borra a los 30 días).

1. Crea una cuenta en <https://neon.com> y un proyecto (región: la más cercana, por ejemplo `us-east`).
2. Copia la cadena de conexión y conviértela a formato JDBC:
   `postgresql://usuario:clave@host/base?sslmode=require` →
   `DB_URL=jdbc:postgresql://host/base?sslmode=require`, `DB_USUARIO=usuario`, `DB_CLAVE=clave`.
   Flyway crea las tablas y la semilla solo, en el primer arranque del backend.

**Servicios — Render.**

1. Crea una cuenta en <https://render.com> con GitHub y autoriza el repositorio.
2. *New → Blueprint* y elige el repositorio: Render lee `render.yaml` y crea los tres servicios.
3. Llena las variables marcadas como `sync: false`:
   - `asistente-api`: `DB_URL`, `DB_USUARIO`, `DB_CLAVE`, `PLN_URL=https://asistente-pln.onrender.com`,
     `CORS_ORIGENES=https://asistente-web.onrender.com`.
   - `asistente-web`: `VITE_API_BASE_URL=https://asistente-api.onrender.com/api`.
   (Si Render asigna otros nombres, usa las URL que muestra el panel de cada servicio.)
4. Cuando el sitio quede arriba, entra a `/admin`, cambia la contraseña del administrador en *Mi cuenta*
   y crea las cuentas reales desde *Usuarios*.

**Limitaciones del plan gratis que hay que conocer (y mencionar en la sustentación):**

- Los servicios web gratis se «duermen» tras 15 minutos sin tráfico y el primer pedido después tarda en
  responder mientras despiertan (un backend Java en 0,1 CPU puede tardar uno o dos minutos). Abre la
  aplicación unos minutos antes de presentar.
- Render da 750 horas de instancia gratis al mes por espacio de trabajo; dos servicios encendidos todo
  el mes se pasarían, por eso conviene dejar que se duerman cuando nadie los usa.
- El sistema de archivos es efímero: el modelo PLN se reentrena solo al arrancar con la base de
  conocimiento de la base de datos, así que no se pierde nada importante.
- Con estos recursos no se cumplen las metas de rendimiento del piloto (RNF-01, RNF-05). Esas metas se
  miden con `docker compose` en un equipo normal o en un servidor como el que pide el documento
  (4 núcleos, 8 GB de RAM).

**Alternativa sin «dormirse»:** una máquina virtual gratuita (por ejemplo, la capa Always Free de Oracle
Cloud, que pide tarjeta para verificar identidad) con Docker instalado, y el mismo
`docker compose up -d --build` de la sección 1. Para HTTPS se pone delante un dominio gratuito y un
certificado de Let's Encrypt.

## Variables de entorno del backend

| Variable | Para qué | Por defecto |
| --- | --- | --- |
| `DB_URL`, `DB_USUARIO`, `DB_CLAVE` | Conexión a PostgreSQL | `localhost:5432/asistente` |
| `PLN_URL` | Dónde está el motor PLN (servicio liviano o Rasa) | `http://localhost:5005` |
| `PLN_TOKEN` | Token para reentrenar el motor | vacío |
| `PLN_TIMEOUT_MS` | Tiempo máximo de espera al PLN | `2500` |
| `JWT_SECRETO` | Firma de los tokens del panel. **Obligatorio cambiarlo** | valor de desarrollo |
| `CORS_ORIGENES` | Dominios del frontend, separados por coma | `http://localhost:5173,http://localhost` |
| `CORREO_HABILITADO`, `SMTP_*` | Envío de correos de escalamiento | deshabilitado |
| `CORREO_DESTINO_ESCALAMIENTO` | Buzón que recibe los escalamientos en el piloto | vacío |
