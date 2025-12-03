import React, { useState, useEffect } from 'react';
import './AdminDashboard.css';

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [usersType, setUsersType] = useState('etudiants');
  const [etudiants, setEtudiants] = useState([]);
  const [enseignants, setEnseignants] = useState([]);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [filterEmail, setFilterEmail] = useState('');
  
  // États pour les formulaires d'ajout
  const [showAddUserForm, setShowAddUserForm] = useState(false);
  const [newUser, setNewUser] = useState({
    nom: '',
    prenom: '',
    email: '',
    password: '',
    departement: '',
    dateNaissance: ''
  });

  // Fonctions pour charger les données
  const fetchEtudiants = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/etudiants', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) throw new Error('Erreur lors du chargement des étudiants');
      
      const data = await response.json();
      setEtudiants(data);
      setError('');
    } catch (err) {
      setError(err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchEnseignants = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/enseignants', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) throw new Error('Erreur lors du chargement des enseignants');
      
      const data = await response.json();
      setEnseignants(data);
      setError('');
    } catch (err) {
      setError(err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchLogs = async (emailFilter = '') => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      let url = 'http://localhost:8080/admin/logs';
      
      if (emailFilter) {
        url = `http://localhost:8080/admin/logs-par-user?email=${encodeURIComponent(emailFilter)}`;
      }
      
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) throw new Error('Erreur lors du chargement des logs');
      
      const data = await response.json();
      setLogs(data);
      setError('');
    } catch (err) {
      setError(err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Fonctions pour gérer les utilisateurs
  const handleAddEtudiant = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/etudiants', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          nom: newUser.nom,
          prenom: newUser.prenom,
          email: newUser.email,
          password: newUser.password,
          departement: newUser.departement,
          dateNaissance: newUser.dateNaissance,
          role: 'ETUDIANT'
        })
      });
      
      if (!response.ok) throw new Error('Erreur lors de l\'ajout de l\'étudiant');
      
      setShowAddUserForm(false);
      setNewUser({ 
        nom: '', 
        prenom: '', 
        email: '', 
        password: '', 
        departement: '', 
        dateNaissance: ''
      });
      fetchEtudiants();
      alert('Étudiant ajouté avec succès');
    } catch (err) {
      setError(err.message);
      console.error(err);
    }
  };

  const handleAddEnseignant = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/enseignants', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          nom: newUser.nom,
          prenom: newUser.prenom,
          email: newUser.email,
          password: newUser.password,
          departement: newUser.departement,
          dateNaissance: newUser.dateNaissance,
          role: 'ENSEIGNANT'
        })
      });
      
      if (!response.ok) throw new Error('Erreur lors de l\'ajout de l\'enseignant');
      
      setShowAddUserForm(false);
      setNewUser({ 
        nom: '', 
        prenom: '', 
        email: '', 
        password: '', 
        departement: '', 
        dateNaissance: ''
      });
      fetchEnseignants();
      alert('Enseignant ajouté avec succès');
    } catch (err) {
      setError(err.message);
      console.error(err);
    }
  };

  const handleDeleteEtudiant = async (id) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer cet étudiant ?')) return;
    
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/etudiants/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) throw new Error('Erreur lors de la suppression');
      
      fetchEtudiants();
    } catch (err) {
      setError(err.message);
      console.error(err);
    }
  };

  const handleDeleteEnseignant = async (id) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer cet enseignant ?')) return;
    
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/enseignants/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) throw new Error('Erreur lors de la suppression');
      
      fetchEnseignants();
    } catch (err) {
      setError(err.message);
      console.error(err);
    }
  };

  const handleFilterLogs = (e) => {
    e.preventDefault();
    fetchLogs(filterEmail);
  };

  useEffect(() => {
    if (activeTab === 'users' && usersType === 'etudiants') {
      fetchEtudiants();
    }
  }, [activeTab, usersType]);

  useEffect(() => {
    if (activeTab === 'users' && usersType === 'enseignants') {
      fetchEnseignants();
    }
  }, [activeTab, usersType]);

  useEffect(() => {
    if (activeTab === 'overview') {
      fetchLogs();
    }
  }, [activeTab]);

  const renderAddUserForm = () => {
    if (!showAddUserForm) {
      return (
        <button 
          className="btn-primary"
          onClick={() => setShowAddUserForm(true)}
        >
          + Ajouter un {usersType === 'etudiants' ? 'étudiant' : 'enseignant'}
        </button>
      );
    }

    return (
      <div className="add-user-form" style={{ marginBottom: '1rem', padding: '1rem', border: '1px solid #ddd', borderRadius: '4px' }}>
        <h3>Ajouter un {usersType === 'etudiants' ? 'étudiant' : 'enseignant'}</h3>
        <form onSubmit={usersType === 'etudiants' ? handleAddEtudiant : handleAddEnseignant}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
            <div>
              <label>Nom</label>
              <input
                type="text"
                value={newUser.nom}
                onChange={(e) => setNewUser({...newUser, nom: e.target.value})}
                required
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label>Prénom</label>
              <input
                type="text"
                value={newUser.prenom}
                onChange={(e) => setNewUser({...newUser, prenom: e.target.value})}
                required
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label>Email</label>
              <input
                type="email"
                value={newUser.email}
                onChange={(e) => setNewUser({...newUser, email: e.target.value})}
                required
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label>Mot de passe</label>
              <input
                type="password"
                value={newUser.password}
                onChange={(e) => setNewUser({...newUser, password: e.target.value})}
                required
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label>Département</label>
              <input
                type="text"
                value={newUser.departement}
                onChange={(e) => setNewUser({...newUser, departement: e.target.value})}
                required
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label>Date de naissance</label>
              <input
                type="date"
                value={newUser.dateNaissance}
                onChange={(e) => setNewUser({...newUser, dateNaissance: e.target.value})}
                required
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
            
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn-primary">
              Ajouter
            </button>
            <button 
              type="button" 
              className="btn-secondary"
              onClick={() => {
                setShowAddUserForm(false);
                setNewUser({ 
                  nom: '', 
                  prenom: '', 
                  email: '', 
                  password: '', 
                  departement: '', 
                  dateNaissance: ''
                });
              }}
            >
              Annuler
            </button>
          </div>
        </form>
      </div>
    );
  };

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
            
            <div className="filter-section" style={{ marginBottom: '1rem' }}>
              <form onSubmit={handleFilterLogs} style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end' }}>
                <div style={{ flex: 1 }}>
                  <label>Filtrer par email</label>
                  <input
                    type="email"
                    value={filterEmail}
                    onChange={(e) => setFilterEmail(e.target.value)}
                    placeholder="Entrez un email pour filtrer..."
                    style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
                  />
                </div>
                <button type="submit" className="btn-primary">
                  Appliquer
                </button>
                <button 
                  type="button" 
                  className="btn-secondary"
                  onClick={() => {
                    setFilterEmail('');
                    fetchLogs();
                  }}
                >
                  Effacer
                </button>
              </form>
            </div>

            {error && (
              <div className="error-message">
                {error}
              </div>
            )}

            {loading ? (
              <div className="loading">Chargement des logs...</div>
            ) : (
              <div className="logs-list">
                <table>
                  <thead>
                    <tr>
                      <th>Date/Heure</th>
                      <th>Type</th>
                      <th>Utilisateur</th>
                      <th>Détails</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logs.length === 0 ? (
                      <tr>
                        <td colSpan="4" style={{ textAlign: 'center', padding: '1rem' }}>
                          Aucun log disponible
                        </td>
                      </tr>
                    ) : (
                      logs.map(log => (
                        <tr key={log.id || log.timestamp}>
                          <td>{new Date(log.timestamp || log.date).toLocaleString()}</td>
                          <td>
                            <span className={`role-badge ${log.type?.toLowerCase() || 'info'}`}>
                              {log.type || 'INCONNU'}
                            </span>
                          </td>
                          <td>{log.userEmail || 'N/A'}</td>
                          <td>{log.details || log.message || 'Aucun détail'}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'users' && (
          <div className="users-management">
            <h2>Gestion des utilisateurs</h2>
            
            <div className="user-type-selector">
              <button 
                className={`btn-secondary ${usersType === 'etudiants' ? 'active' : ''}`}
                onClick={() => setUsersType('etudiants')}
              >
                Étudiants
              </button>
              <button 
                className={`btn-secondary ${usersType === 'enseignants' ? 'active' : ''}`}
                onClick={() => setUsersType('enseignants')}
              >
                Enseignants
              </button>
            </div>

            <div className="management-actions">
              {renderAddUserForm()}
              <button 
                className="btn-primary"
                onClick={usersType === 'etudiants' ? fetchEtudiants : fetchEnseignants}
                style={{ marginLeft: '0.5rem' }}
              >
                Rafraîchir la liste
              </button>
            </div>

            {error && (
              <div className="error-message">
                {error}
              </div>
            )}

            {loading ? (
              <div className="loading">Chargement...</div>
            ) : (
              <div className="users-list">
                {usersType === 'etudiants' ? (
                  <>
                    <h3>Liste des étudiants ({etudiants.length})</h3>
                    <table>
                      <thead>
                        <tr>
                          <th>Nom</th>
                          <th>Prénom</th>
                          <th>Email</th>
                          <th>Département</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {etudiants.map(etudiant => (
                          <tr key={etudiant.id}>
                            <td>{etudiant.nom}</td>
                            <td>{etudiant.prenom}</td>
                            <td>{etudiant.email}</td>
                            <td>{etudiant.departement}</td>
                            <td>
                              <button 
                                className="btn-delete"
                                onClick={() => handleDeleteEtudiant(etudiant.id)}
                              >
                                Supprimer
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </>
                ) : (
                  <>
                    <h3>Liste des enseignants ({enseignants.length})</h3>
                    <table>
                      <thead>
                        <tr>
                          <th>Nom</th>
                          <th>Prénom</th>
                          <th>Email</th>
                          <th>Département</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {enseignants.map(enseignant => (
                          <tr key={enseignant.id}>
                            <td>{enseignant.nom}</td>
                            <td>{enseignant.prenom}</td>
                            <td>{enseignant.email}</td>
                            <td>{enseignant.departement || 'Non spécifié'}</td>
                            <td>
                              <button 
                                className="btn-delete"
                                onClick={() => handleDeleteEnseignant(enseignant.id)}
                              >
                                Supprimer
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;