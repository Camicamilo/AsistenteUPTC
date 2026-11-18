import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { adminService } from '../services/adminService.js';
import { TOKEN_KEY } from '../services/apiClient.js';
import { decodeJwt, tokenVigente } from '../utils/jwt.js';
import { sessionStore } from '../utils/storage.js';

const AuthContext = createContext(null);

function usuarioDesdeToken(token) {
  if (!token || !tokenVigente(token)) return null;
  const p = decodeJwt(token);
  return { id: p.sub, nombre: p.nombre, correo: p.correo, rol: p.rol, exp: p.exp };
}

// Sesión del panel administrativo con JWT (expira a las 8 horas, Tabla 30).
export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => {
    const t = sessionStore.get(TOKEN_KEY);
    return t && tokenVigente(t) ? t : null;
  });
  const [motivoSalida, setMotivoSalida] = useState(null);
  const usuario = useMemo(() => usuarioDesdeToken(token), [token]);

  const logout = useCallback((motivo = null) => {
    sessionStore.remove(TOKEN_KEY);
    setToken(null);
    setMotivoSalida(motivo);
  }, []);

  const login = useCallback(async (correo, password) => {
    const r = await adminService.login(correo, password);
    sessionStore.set(TOKEN_KEY, r.token);
    setMotivoSalida(null);
    setToken(r.token);
    return r.usuario;
  }, []);

  // Cierre automático cuando el token expira.
  useEffect(() => {
    if (!usuario?.exp) return undefined;
    const ms = usuario.exp * 1000 - Date.now();
    const id = setTimeout(() => logout('Tu sesión expiró. Inicia sesión de nuevo.'), Math.max(0, ms));
    return () => clearTimeout(id);
  }, [usuario, logout]);

  // Para usar en los catch de las páginas: un 401 cierra la sesión.
  const manejarError = useCallback(
    (e) => {
      if (e?.codigo === 'NO_AUTORIZADO') logout('Tu sesión expiró. Inicia sesión de nuevo.');
    },
    [logout],
  );

  const value = useMemo(
    () => ({
      usuario,
      login,
      logout,
      manejarError,
      motivoSalida,
    }),
    [usuario, login, logout, manejarError, motivoSalida],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
