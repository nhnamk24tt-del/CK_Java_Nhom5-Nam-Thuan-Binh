import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import AdminDashboard from "./AdminDashboard.jsx";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Redirect từ "/" sang "/admin/dashboard" */}
        <Route path="/" element={<Navigate to="/admin/dashboard" />} />
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
