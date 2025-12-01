import React, { useState } from 'react';
import './EtudiantDashboard.css';

const EtudiantDashboard = () => {
  const [activeTab, setActiveTab] = useState('examens-a-venir');

  return (
    <div className="etudiant-dashboard">
      <div className="dashboard-tabs">
        <button 
          className={activeTab === 'examens-a-venir' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('examens-a-venir')}
        >
          Examens à venir
        </button>
        <button 
          className={activeTab === 'examens-passes' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('examens-passes')}
        >
          Examens passés
        </button>
        <button 
          className={activeTab === 'resultats' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('resultats')}
        >
          Mes résultats
        </button>
      </div>

      <div className="tab-content">
        {activeTab === 'examens-a-venir' && (
          <div className="examens-a-venir">
            <h2>Examens à venir</h2>
            <div className="examens-list">
              

            </div>
          </div>
        )}

        {activeTab === 'examens-passes' && (
          <div className="examens-passes">
            <h2>Examens passés</h2>
            <div className="examens-list">
              

            </div>
          </div>
        )}

        {activeTab === 'resultats' && (
          <div className="mes-resultats">
            <h2>Mes résultats</h2>
            <div className="results-summary">
              

            </div>
          </div>
        )}

      </div>
    </div>
  );
};

export default EtudiantDashboard;