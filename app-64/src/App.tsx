import { Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/contexts/AuthContext';
import { GameConfigProvider } from '@/contexts/GameConfigContext';
import { Toaster } from '@/components/ui/toaster';
import Header from '@/components/common/Header';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { Loader2 } from 'lucide-react';
import routes from './routes';
import { SoundPreferenceSync } from '@/features/settings/SoundPreferenceSync';

function LoadingFallback() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <Loader2 className="w-8 h-8 animate-spin text-primary" />
    </div>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <GameConfigProvider>
          <SoundPreferenceSync />
          <div className="flex flex-col min-h-screen">
            <Header />
            <main className="flex-grow">
              <Suspense fallback={<LoadingFallback />}>
                <Routes>
                  {routes.map((route) => {
                    const Element = route.element;
                    return (
                      <Route
                        key={route.path}
                        path={route.path}
                        element={
                          <ProtectedRoute
                            requireAuth={route.requireAuth}
                            requireAdmin={route.requireAdmin}
                          >
                            <Element />
                          </ProtectedRoute>
                        }
                      />
                    );
                  })}
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
              </Suspense>
            </main>
          </div>
          <Toaster />
        </GameConfigProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
