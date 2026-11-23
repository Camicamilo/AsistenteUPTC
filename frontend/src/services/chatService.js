// Punto único de acceso al chat: usa el simulador o la API real según VITE_USE_MOCK.
import { config } from '../config.js';
import { chatApi } from './chatApi.js';
import { mockChat } from './mock/mockChat.js';

export const chatService = config.useMock ? mockChat : chatApi;
export const modoDemo = config.useMock;
