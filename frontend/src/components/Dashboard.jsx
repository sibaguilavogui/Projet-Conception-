import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import AdminDashboard from './AdminDashboard.jsx';
import EnseignantDashboard from './EnseignantDashboard';
import EtudiantDashboard from './EtudiantDashboard';
import './Dashboard.css';

const Dashboard = () => {
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    try {
      const token = localStorage.getItem('token');
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        credentials: 'include'
      });
    } catch (error) {
      console.error('Erreur lors de la déconnexion:', error);
    } finally {
      logout();
    }
  };

  const renderDashboard = () => {
    switch (user?.role) {
      case 'ADMIN':
        return <AdminDashboard />;
      case 'ENSEIGNANT':
        return <EnseignantDashboard />;
      case 'ETUDIANT':
        return <EtudiantDashboard />;
      default:
        return <div>Rôle non reconnu</div>;
    }
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>EXAM-GU - Tableau de bord</h1>
          <div className="user-info">
            <span>Bienvenue, {user?.prenom} {user?.nom} ({user?.role})</span>
            <button onClick={handleLogout} className="logout-btn">
              Déconnexion
            </button>
          </div>
        </div>
      </header>
      <main className="dashboard-main">
        {renderDashboard()}
      </main>
    </div>
  );
};

export default Dashboard;