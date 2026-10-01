// Acceso seguro a localStorage / sessionStorage: puede fallar en modo privado o con el
// almacenamiento bloqueado, así que todo va envuelto en try/catch.
function make(getStore) {
  return {
    get(key, fallback = null) {
      try {
        const raw = getStore().getItem(key);
        return raw == null ? fallback : JSON.parse(raw);
      } catch {
        return fallback;
      }
    },
    set(key, value) {
      try {
        getStore().setItem(key, JSON.stringify(value));
      } catch {
        /* sin almacenamiento disponible: se ignora */
      }
    },
    remove(key) {
      try {
        getStore().removeItem(key);
      } catch {
        /* ignorado */
      }
    },
  };
}

export const storage = make(() => window.localStorage);
export const sessionStore = make(() => window.sessionStorage);
