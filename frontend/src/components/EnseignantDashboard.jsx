import React, { useState } from 'react';
import './EnseignantDashboard.css';

const EnseignantDashboard = () => {
  const [activeTab, setActiveTab] = useState('mes-examens');

  return (
    <div className="enseignant-dashboard">
      <div className="dashboard-tabs">
        <button 
          className={activeTab === 'mes-examens' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('mes-examens')}
        >
          Mes examens
        </button>
        <button 
          className={activeTab === 'creer-examen' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('creer-examen')}
        >
          Créer un examen
        </button>
        
      </div>

      <div className="tab-content">
        {activeTab === 'mes-examens' && (
          <div className="mes-examens">
            <h2>Mes examens</h2>
            
          </div>
        )}

        {activeTab === 'creer-examen' && (
          <div className="creer-examen">
            <h2>Créer un nouvel examen</h2>
            <form className="examen-form">
              
              <button type="submit" className="btn-primary">Créer l'examen</button>
            </form>
          </div>
        )}

      </div>
    </div>
  );
};

export default EnseignantDashboard;