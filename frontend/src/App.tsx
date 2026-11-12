import { Navigate, Route, Routes } from 'react-router-dom';
import { useEffect } from 'react';


import ChatPage from './pages/ChatPage';
import LoginPage from './pages/LoginPage';
import SignUpPage from './pages/SignUpPage';

import { useAuthStore } from './store/useAuthStore';

import PageLoader from './components/PageLoader';
import { Toaster } from 'react-hot-toast';

function AppContent() {
  const { checkAuth, isCheckingAuth, authUser } = useAuthStore();
  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  if (isCheckingAuth) return <PageLoader />;
  return (
    <>
      <Routes>
        {/* PUBLIC ROUTES */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={!authUser ? <LoginPage /> : <Navigate to="/chat" />} />
        <Route path="/signup" element={!authUser ? <SignUpPage /> : <Navigate to="/chat" />} />
        {/* PROTECTED ROUTES */}
        <Route path="/chat" element={authUser ? <ChatPage /> : <Navigate to="/login" />} />
      </Routes>
      <Toaster />
    </>
  );
}

function App() {
  const { authUser } = useAuthStore();
  console.log("Current authUser:", authUser);
  return <AppContent />;
}

export default App;