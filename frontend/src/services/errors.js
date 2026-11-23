// Errores normalizados que la interfaz sabe mostrar (RNF-08: ningún error deja el chat congelado).
export class ServiceError extends Error {
  /**
   * @param {'TIMEOUT'|'RED'|'PLN_NO_DISPONIBLE'|'NO_AUTORIZADO'|'PROHIBIDO'|'VALIDACION'|'CONFLICTO'|'SERVIDOR'} codigo
   */
  constructor(codigo, mensaje, detalles) {
    super(mensaje);
    this.name = 'ServiceError';
    this.codigo = codigo;
    this.detalles = detalles;
  }
}

export const MENSAJES_ERROR = {
  TIMEOUT: 'La respuesta está tardando más de lo normal. Intenta de nuevo en unos segundos.',
  RED: 'En este momento no puedo procesar tu consulta. Revisa tu conexión e intenta de nuevo.',
  PLN_NO_DISPONIBLE:
    'El servicio del asistente no está disponible temporalmente. Mientras se restablece, puedes comunicarte con atención al ciudadano.',
  SERVIDOR: 'Ocurrió un error inesperado. Intenta de nuevo en unos segundos.',
};
