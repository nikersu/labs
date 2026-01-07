import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './components/common/NotificationContext'
import Layout from './components/common/Layout'
import LoginForm from './components/auth/LoginForm'
import RegisterForm from './components/auth/RegisterForm'
import FunctionList from './components/functions/FunctionList'
import FunctionForm from './components/functions/FunctionForm'
import FunctionEditForm from './components/functions/FunctionEditForm'
import ProtectedRoute from './components/common/ProtectedRoute'

function App() {
  return (
    <Router>
      <AuthProvider>
        <NotificationProvider>
          <Routes>
            <Route path="/login" element={<LoginForm />} />
            <Route path="/register" element={<RegisterForm />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/functions" replace />} />
              <Route path="functions" element={<FunctionList />} />
              <Route path="functions/new" element={<FunctionForm />} />
              <Route path="functions/:id/edit" element={<FunctionEditForm />} />
            </Route>
          </Routes>
        </NotificationProvider>
      </AuthProvider>
    </Router>
  )
}

export default App





