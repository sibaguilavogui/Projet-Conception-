import React, { useState } from 'react';
import './AdminDashboard.css';

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('overview');

  return (
    <div className="admin-dashboard">
      <div className="dashboard-tabs">
        <button 
          className={activeTab === 'overview' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('overview')}
        >
          Activités (Logs)
        </button>
        <button 
          className={activeTab === 'users' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('users')}
        >
          Gestion des utilisateurs
        </button>
      </div>

      <div className="tab-content">
        {activeTab === 'overview' && (
          <div className="overview">
            <h2>Activités (Logs)</h2>

          </div>
        )}

        {activeTab === 'users' && (
          <div className="users-management">
            <h2>Gestion des utilisateurs</h2>
            <div className="management-actions">
              <button className="btn-primary">Ajouter un utilisateur</button>
              <button className="btn-secondary">Importer des utilisateurs</button>
            </div>
            <div className="users-list">
              
            </div>
          </div>
        )}

      </div>
    </div>
  );
};

export default AdminDashboard;