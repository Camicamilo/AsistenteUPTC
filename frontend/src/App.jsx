import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import AdminLayout from './components/admin/AdminLayout.jsx';
import { AuthProvider, useAuth } from './hooks/useAuth.jsx';
import DashboardPage from './pages/admin/DashboardPage.jsx';
import KnowledgeBasePage from './pages/admin/KnowledgeBasePage.jsx';
import LoginPage from './pages/admin/LoginPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import PortalDemoPage from './pages/PortalDemoPage.jsx';

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
