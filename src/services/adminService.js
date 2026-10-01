// Punto único de acceso al panel: simulador o API real según VITE_USE_MOCK.
import { config } from '../config.js';
import { adminApi } from './adminApi.js';
import { mockAdmin } from './mock/mockAdmin.js';

export const adminService = config.useMock ? mockAdmin : adminApi;
