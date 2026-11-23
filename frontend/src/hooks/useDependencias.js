import { useEffect, useState } from 'react';
import { LISTA_DEPENDENCIAS } from '../pages/admin/constantes.js';
import { adminService } from '../services/adminService.js';

// Dependencias para los selectores del panel. Arranca con la lista local y se actualiza con la del
// servidor (el superadministrador puede cambiar nombres y contactos desde Configuración).
export function useDependencias() {
  const [lista, setLista] = useState(LISTA_DEPENDENCIAS);
  useEffect(() => {
    let vigente = true;
    adminService
      .dependencias?.()
      .then((d) => vigente && Array.isArray(d) && d.length && setLista(d))
      .catch(() => {});
    return () => {
      vigente = false;
    };
  }, []);
  return lista;
}
