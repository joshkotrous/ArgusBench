import { Link, Route, Routes, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import { useAuth } from './auth/AuthContext'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import Transactions from './pages/Transactions'
import Reports from './pages/Reports'
import Admin from './pages/Admin'

export default function App() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'admin'

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', padding: 16 }}>
      <header style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        <Link to="/">Dashboard</Link>
        <Link to="/accounts">Accounts</Link>
        <Link to="/transactions">Transactions</Link>
        <Link to="/reports">Reports</Link>
        <Link to="/admin">Admin</Link>
        <Link to="/login" style={{ marginLeft: 'auto' }}>Login</Link>
      </header>
      <Routes>
        <Route path="/" element={user ? <Dashboard /> : <Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/accounts" element={user ? <Accounts /> : <Navigate to="/login" replace />} />
        <Route path="/transactions" element={user ? <Transactions /> : <Navigate to="/login" replace />} />
        <Route path="/reports" element={user ? <Reports /> : <Navigate to="/login" replace />} />
        <Route path="/admin" element={isAdmin ? <Admin /> : <Navigate to="/login" replace />} />
      </Routes>
    </div>
  )
}
