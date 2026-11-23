# ---- Etapa 1: compilar React con Vite ----
FROM node:20-alpine AS compilacion
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# En contenedor el frontend usa la API real (no el simulador)
ARG VITE_USE_MOCK=false
ARG VITE_API_BASE_URL=/api
ENV VITE_USE_MOCK=$VITE_USE_MOCK VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- Etapa 2: Nginx sirve los archivos estáticos y hace de proxy hacia el backend ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=compilacion /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=3s --retries=5 CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
