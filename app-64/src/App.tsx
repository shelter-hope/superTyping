import { Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from '@/contexts/AuthContext';
import { GameConfigProvider } from '@/contexts/GameConfigContext';
import { Toaster } from '@/components/ui/toaster';
import Header from '@/components/common/Header';
import Footer from '@/components/common/Footer';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import routes from './routes';
import { SoundPreferenceSync } from '@/features/settings/SoundPreferenceSync';

function LoadingFallback() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4">
      <p className="label-mono text-[10px]">载入中</p>
      <span className="h-[1px] w-24 overflow-hidden bg-border">
        <span className="block h-full w-1/3 animate-loading-sweep bg-foreground" />
      </span>
    </div>
  );
}

function AppShell() {
  const location = useLocation();
  // 对局与聊天是沉浸式页面：不挂页脚
  const immersive =
    location.pathname.startsWith('/game') || location.pathname.startsWith('/chat');

  return (
    <div className="flex min-h-screen flex-col">
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
      {!immersive && <Footer />}
    </div>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <GameConfigProvider>
          <SoundPreferenceSync />
          <AppShell />
          <Toaster />
        </GameConfigProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
