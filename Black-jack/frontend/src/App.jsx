import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage, { RequireAuth } from './pages/LoginPage'
import LobbyPage from './pages/LobbyPage'
import TablePage from './pages/TablePage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route
        path="/lobby"
        element={
          <RequireAuth>
            <LobbyPage />
          </RequireAuth>
        }
      />
      <Route
        path="/table/:tableId"
        element={
          <RequireAuth>
            <TablePage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}
