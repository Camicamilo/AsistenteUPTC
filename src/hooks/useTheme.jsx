import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { storage } from '../utils/storage.js';

const ThemeContext = createContext({ theme: 'system', setTheme: () => {} });
const KEY = 'uptc-av-theme';

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(() => storage.get(KEY, 'system'));

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'light' || theme === 'dark') root.setAttribute('data-theme', theme);
    else root.removeAttribute('data-theme');
  }, [theme]);

  const setTheme = useCallback((value) => {
    setThemeState(value);
    storage.set(KEY, value);
  }, []);

  return <ThemeContext.Provider value={{ theme, setTheme }}>{children}</ThemeContext.Provider>;
}

export const useTheme = () => useContext(ThemeContext);
