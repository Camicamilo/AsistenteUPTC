import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import AdminLayout from './components/admin/AdminLayout.jsx';
import { AuthProvider, useAuth } from './hooks/useAuth.jsx';
import ConfiguracionPage from './pages/admin/ConfiguracionPage.jsx';
import CuentaPage from './pages/admin/CuentaPage.jsx';
import DashboardPage from './pages/admin/DashboardPage.jsx';
import HistorialPage from './pages/admin/HistorialPage.jsx';
import KnowledgeBasePage from './pages/admin/KnowledgeBasePage.jsx';
import LoginPage from './pages/admin/LoginPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import PortalDemoPage from './pages/PortalDemoPage.jsx';
import UsuariosPage from './pages/admin/UsuariosPage.jsx';

function SoloSuper({ children }) {
  const { usuario } = useAuth();
  return usuario?.esSuper ? children : <Navigate to="/admin" replace />;
}

function RequireAuth({ children }) {
  const { usuario } = useAuth();
  const location = useLocation();
  if (!usuario) return <Navigate to="/admin/login" replace state={{ desde: location.pathname }} />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<PortalDemoPage />} />
      <Route
        path="/admin/*"
        element={
          <AuthProvider>
            <Routes>
              <Route path="login" element={<LoginPage />} />
              <Route
                element={
                  <RequireAuth>
                    <AdminLayout />
                  </RequireAuth>
                }
              >
                <Route index element={<DashboardPage />} />
                <Route path="conocimiento" element={<KnowledgeBasePage />} />
                <Route path="historial" element={<HistorialPage />} />
                <Route path="cuenta" element={<CuentaPage />} />
                <Route
                  path="usuarios"
                  element={
                    <SoloSuper>
                      <UsuariosPage />
                    </SoloSuper>
                  }
                />
                <Route
                  path="configuracion"
                  element={
                    <SoloSuper>
                      <ConfiguracionPage />
                    </SoloSuper>
                  }
                />
                <Route path="*" element={<Navigate to="/admin" replace />} />
              </Route>
            </Routes>
          </AuthProvider>
        }
      />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
