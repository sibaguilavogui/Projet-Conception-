import React, { useState, useEffect } from 'react';
import './AdminDashboard.css';

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [usersType, setUsersType] = useState('etudiants');
  const [etudiants, setEtudiants] = useState([]);
  const [enseignants, setEnseignants] = useState([]);
  const [examens, setExamens] = useState([]);
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

  // États pour la gestion des examens
  const [selectedExamen, setSelectedExamen] = useState(null);
  const [inscriptions, setInscriptions] = useState([]);
  const [etudiantAInscrire, setEtudiantAInscrire] = useState('');
  const [showExamenDetails, setShowExamenDetails] = useState(false);

  // Fonctions pour charger les données
  const fetchEtudiants = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const response = await fetch('/api/etudiants', {
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
      const response = await fetch('/api/enseignants', {
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

  const fetchExamens = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const response = await fetch('/api/examens', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) throw new Error('Erreur lors du chargement des examens');
      
      const data = await response.json();
      setExamens(data);
      setError('');
    } catch (err) {
      setError(err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchInscriptions = async (examenId) => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/examens/${examenId}/inscriptions`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        setInscriptions([]);
        return;
      }
      
      const data = await response.json();
      setInscriptions(data);
    } catch (err) {
      console.error('Erreur lors du chargement des inscriptions:', err);
      setInscriptions([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchLogs = async (emailFilter = '') => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      let url = '/api/admin/logs';
      
      if (emailFilter) {
        url = `/api/admin/logs-par-user?email=${encodeURIComponent(emailFilter)}`;
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
      const response = await fetch('/api/etudiants', {
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
      const response = await fetch('/api/enseignants', {
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
      const response = await fetch(`/api/etudiants/${id}`, {
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
      const response = await fetch(`/api/enseignants/${id}`, {
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

  // Fonctions pour gérer les examens et inscriptions
  const handleSelectExamen = async (examen) => {
    setSelectedExamen(examen);
    setShowExamenDetails(true);
    if (etudiants.length === 0) {
      await fetchEtudiants();
    }
    await fetchInscriptions(examen.id);
  };

  const handleInscrireEtudiant = async (e) => {
    e.preventDefault();
    if (!etudiantAInscrire || !selectedExamen) return;

    try {
      const token = localStorage.getItem('token');
      const etudiant = etudiants.find(e => e.email === etudiantAInscrire);
      if (!etudiant) {
        alert('Étudiant non trouvé. Veuillez d\'abord créer l\'étudiant.');
        return;
      }

      const response = await fetch(`/api/examens/${selectedExamen.id}/${etudiant.id}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) throw new Error('Erreur lors de l\'inscription');
      
      setEtudiantAInscrire('');
      alert('Étudiant inscrit avec succès');
      fetchInscriptions(selectedExamen.id);
    } catch (err) {
      setError(err.message);
      console.error(err);
    }
  };

  const handleDesinscrireEtudiant = async (etudiantId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir désinscrire cet étudiant ?')) return;
    
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/examens/${selectedExamen.id}/${etudiantId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) throw new Error('Erreur lors de la désinscription');
      
      alert('Étudiant désinscrit avec succès');
      fetchInscriptions(selectedExamen.id);
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
    if (activeTab === 'examens') {
      fetchExamens();
    }
  }, [activeTab]);

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
        <button 
          className={activeTab === 'examens' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('examens')}
        >
          Gestion des inscriptions aux examens
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

        {activeTab === 'examens' && (
          <div className="examens-management">
            <h2>Gestion des examens</h2>
            
            <div className="management-actions">
              <button 
                className="btn-primary"
                onClick={fetchExamens}
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
              <div className="loading">Chargement des examens...</div>
            ) : (
              <>
                <h3>Liste des examens ({examens.length})</h3>
                <table>
                  <thead>
                    <tr>
                      <th>Titre</th>
                      <th>État</th>
                      <th>Créateur</th>
                      <th>Date début</th>
                      <th>Date fin</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {examens.map(examen => (
                      <tr key={examen.id}>
                        <td>{examen.titre}</td>
                        <td>
                          <span className={`role-badge ${examen.etat?.toLowerCase() || 'info'}`}>
                            {examen.etat || 'INCONNU'}
                          </span>
                        </td>
                        <td>{examen.createur?.email || 'N/A'}</td>
                        <td>{examen.dateDebut ? new Date(examen.dateDebut).toLocaleString() : 'N/A'}</td>
                        <td>{examen.dateFin ? new Date(examen.dateFin).toLocaleString() : 'N/A'}</td>
                        <td>
                          <button 
                            className="btn-edit"
                            onClick={() => handleSelectExamen(examen)}
                            style={{ marginRight: '0.5rem' }}
                          >
                            Gérer inscriptions
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {showExamenDetails && selectedExamen && (
                  <div style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 1000
                  }}>
                    <div style={{
                      backgroundColor: 'white',
                      padding: '2rem',
                      borderRadius: '8px',
                      maxWidth: '800px',
                      width: '90%',
                      maxHeight: '90vh',
                      overflowY: 'auto'
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                        <h3>Gestion des inscriptions - {selectedExamen.titre}</h3>
                        <button 
                          className="btn-secondary"
                          onClick={() => {
                            setShowExamenDetails(false);
                            setSelectedExamen(null);
                            setEtudiantAInscrire('');
                          }}
                        >
                          Fermer
                        </button>
                      </div>

                      <div style={{ marginBottom: '2rem' }}>
                        <h4>Ajouter un étudiant</h4>
                        <form onSubmit={handleInscrireEtudiant} style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end' }}>
                          <div style={{ flex: 1 }}>
                            <label>Email de l'étudiant</label>
                            <input
                              type="email"
                              value={etudiantAInscrire}
                              onChange={(e) => setEtudiantAInscrire(e.target.value)}
                              placeholder="Entrez l'email de l'étudiant"
                              style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
                              required
                            />
                          </div>
                          <button type="submit" className="btn-primary">
                            Inscrire
                          </button>
                        </form>
                      </div>

                      <div>
                        <h4>Étudiants inscrits ({inscriptions.length})</h4>
                        {inscriptions.length === 0 ? (
                          <p>Aucun étudiant inscrit à cet examen.</p>
                        ) : (
                          <table>
                            <thead>
                              <tr>
                                <th>Nom</th>
                                <th>Prénom</th>
                                <th>Email</th>
                                <th>Statut</th>
                                <th>Actions</th>
                              </tr>
                            </thead>
                            <tbody>
                              {inscriptions.map(inscription => (
                                <tr key={inscription.id}>
                                  <td>{inscription.etudiant?.nom || 'N/A'}</td>
                                  <td>{inscription.etudiant?.prenom || 'N/A'}</td>
                                  <td>{inscription.etudiant?.email || 'N/A'}</td>
                                  <td>
                                    <span className={`role-badge ${inscription.statut?.toLowerCase() || 'info'}`}>
                                      {inscription.statut || 'INCONNU'}
                                    </span>
                                  </td>
                                  <td>
                                    <button 
                                      className="btn-delete"
                                      onClick={() => handleDesinscrireEtudiant(inscription.etudiant?.id)}
                                    >
                                      Désinscrire
                                    </button>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;