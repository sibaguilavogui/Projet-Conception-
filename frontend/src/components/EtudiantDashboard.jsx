import React, { useState, useEffect, useCallback } from 'react';
import ExamenComposition from './ExamenComposition';
import SoumissionConfirmee from './SoumissionConfirmee';
import './EtudiantDashboard.css';

const EtudiantDashboard = () => {
  // États principaux
  const [activeTab, setActiveTab] = useState('examens-disponibles');
  const [examens, setExamens] = useState([]);
  const [examensAvecStatut, setExamensAvecStatut] = useState([]);
  const [resultats, setResultats] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [examenResultat, setExamenResultat] = useState(null);
  const [tentativeResultat, setTentativeResultat] = useState(null);
  
  // États pour la gestion des examens
  const [etapeExamen, setEtapeExamen] = useState(null);
  const [examenEnCours, setExamenEnCours] = useState(null);
  const [tentativeEnCours, setTentativeEnCours] = useState(null);
  const [reponses, setReponses] = useState({});
  
  const verifierStatutExamen = async (examen) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `/api/examens/${examen.id}/verifier-tentative`,
        {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      if (response.ok) {
        const data = await response.json();
        return data;
      }
      return null;
    } catch (err) {
      console.error('Erreur lors de la vérification du statut:', err);
      return null;
    }
  };

  const chargerExamensAvecStatuts = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/examens', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setExamens(data);
        
        const examensAvecStatuts = await Promise.all(
          data.map(async (examen) => {
            const statutData = await verifierStatutExamen(examen);
            
            const maintenant = new Date();
            const debut = examen.dateDebut ? new Date(examen.dateDebut) : null;
            const fin = examen.dateFin ? new Date(examen.dateFin) : null;
            
            let statut = 'À venir';
            let badge = 'pret';
            let disponible = false;
            
            if (statutData?.tentativeExistante) {
              if (statutData.statut === 'SOUMISE' || statutData.statut === 'EXPIREE') {
                statut = 'Terminé';
                badge = 'termine';
                disponible = false;
              } else if (statutData.statut === 'EN_COURS') {
                if (statutData.estExpiree) {
                  statut = 'Terminé';
                  badge = 'termine';
                  disponible = false;
                } else {
                  statut = 'En cours';
                  badge = 'en-cours';
                  disponible = debut && fin && maintenant >= debut && maintenant <= fin;
                }
              }
            } else {
              if (!debut || !fin) {
                statut = 'Non planifié';
                badge = 'brouillon';
                disponible = false;
              } else if (maintenant < debut) {
                statut = 'À venir';
                badge = 'pret';
                disponible = false;
              } else if (maintenant >= debut && maintenant <= fin) {
                statut = 'Disponible';
                badge = 'disponible';
                disponible = true;
              } else if (maintenant > fin) {
                statut = 'Terminé';
                badge = 'termine';
                disponible = false;
              }
            }
            
            return {
              ...examen,
              statut,
              badge,
              disponible,
              statutData
            };
          })
        );
        
        setExamensAvecStatut(examensAvecStatuts);
      } else {
        throw new Error('Erreur lors du chargement des examens');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const chargerResultats = async () => {
    try {
      const token = localStorage.getItem('token');
      
      // Option 1: Récupérer toutes les tentatives de l'étudiant
      // Note: Cet endpoint retourne toutes les tentatives
      const response = await fetch('/api/tentatives', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const toutesTentatives = await response.json();
        
        // Filtrer pour ne garder que les tentatives soumises ou expirées
        const tentativesFiltrees = toutesTentatives.filter(t => 
          t.statut === 'SOUMISE' || t.statut === 'EXPIREE'
        );
        
        // Transformer les données pour correspondre à la structure attendue
        const resultatsFormates = await Promise.all(
          tentativesFiltrees.map(async (tentative) => {
            // Pour chaque tentative, récupérer l'examen associé
            const examenResponse = await fetch(
              `/api/examens/${tentative.examen?.id}`,
              {
                method: 'GET',
                headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
                }
              }
            );
            
            let examen = null;
            if (examenResponse.ok) {
              examen = await examenResponse.json();
            }
            
            return {
              id: tentative.id,
              score: tentative.noteFinale || 0,
              statut: tentative.statut,
              estCorrigee: tentative.estCorrigee || false,
              dateSoumission: tentative.dateSoumission || tentative.dateCreation,
              examen: examen || tentative.examen,
              tentativeId: tentative.id
            };
          })
        );
        
        setResultats(resultatsFormates);
      }
    } catch (err) {
      console.error('Erreur lors du chargement des résultats:', err);
      setError('Impossible de charger les résultats');
    }
  };

  const verifierNotesVisibles = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `/api/examens/${examenId}`,
        {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      if (response.ok) {
        const examen = await response.json();
        return examen.notesVisibles || false;
      }
      return false;
    } catch (err) {
      console.error('Erreur lors de la vérification des notes:', err);
      return false;
    }
  };

  const demarrerOuReprendreExamen = (examen) => {
    setExamenEnCours(examen);
    setActiveTab(null);
    verifierEtDemarrerTentative(examen);
  };

  const verifierEtDemarrerTentative = async (examen) => {
    try {
      const token = localStorage.getItem('token');
      
      const verificationResponse = await fetch(
        `/api/examens/${examen.id}/verifier-tentative`,
        {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      if (verificationResponse.ok) {
        const verificationData = await verificationResponse.json();
        
        if (verificationData.tentativeExistante) {
          // Il y a une tentative existante
          const tentativeStatut = verificationData.statut;
          
          if (tentativeStatut === 'EN_COURS' && !verificationData.estExpiree) {
            // Tentative en cours non expirée : récupérer la tentative
            const reprendreResponse = await fetch(
              `/api/tentatives/${verificationData.tentativeId}/reprendre`,
              {
                method: 'GET',
                headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
                }
              }
            );
            
            if (reprendreResponse.ok) {
              const reprendreData = await reprendreResponse.json();
              setTentativeEnCours(reprendreData.tentative);
              setExamenEnCours(examen);
              setEtapeExamen('composition');
            }
          } else if (tentativeStatut === 'SOUMISE') {
            const tentativeResponse = await fetch(
              `/api/tentatives/${verificationData.tentativeId}`,
              {
                method: 'GET',
                headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
                }
              }
            );
            
            if (tentativeResponse.ok) {
              const tentativeData = await tentativeResponse.json();
              setTentativeEnCours(tentativeData);
              setExamenEnCours(examen);
              setEtapeExamen('confirmation');
            }
          } else if (tentativeStatut === 'EN_COURS' && verificationData.estExpiree) {
            // Tentative expirée : soumettre automatiquement
            const soumissionResponse = await fetch(
              `/api/tentatives/${verificationData.tentativeId}/soumettre`,
              {
                method: 'POST',
                headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
                }
              }
            );
            
            if (soumissionResponse.ok) {
              const soumissionData = await soumissionResponse.json();
              setTentativeEnCours(soumissionData.tentative);
              setExamenEnCours(examen);
              setEtapeExamen('confirmation');
              setSuccessMessage('Votre examen expiré a été soumis automatiquement.');
            }
          }
        } else {
          // Pas de tentative existante, en démarrer une nouvelle
          const demarrerResponse = await fetch(
            `/api/examens/${examen.id}/demarrer-tentative`,
            {
              method: 'POST',
              headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
              }
            }
          );
          
          if (demarrerResponse.ok) {
            const demarrerData = await demarrerResponse.json();
            setTentativeEnCours(demarrerData.tentative);
            setExamenEnCours(examen);
            setEtapeExamen('composition');
          }
        }
      }
    } catch (err) {
      setError(err.message);
      setExamenEnCours(null);
      setEtapeExamen(null);
      setActiveTab('examens-disponibles');
    }
  };

  // Soumettre l'examen
  const soumettreExamen = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/tentatives/${tentativeEnCours.id}/soumettre`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setTentativeEnCours(data.tentative);
        setEtapeExamen('confirmation');
        setSuccessMessage('Examen soumis avec succès !');
        
        chargerExamensAvecStatuts();
      } else {
        const errorData = await response.text();
        throw new Error(errorData || 'Erreur lors de la soumission');
      }
    } catch (err) {
      setError(err.message);
    }
  };

  const quitterExamen = () => {
    if (window.confirm('Êtes-vous sûr de vouloir quitter l\'examen ? Vos réponses seront sauvegardées et vous pourrez reprendre plus tard.')) {
      resetExamen();
    }
  };

  const terminerExamen = () => {
    resetExamen();
    chargerExamensAvecStatuts();
    setExamenResultat(null);
    setTentativeResultat(null);
  };

  const fermerResultatDetail = () => {
    setExamenResultat(null);
    setTentativeResultat(null);
    setEtapeExamen(null);
    setActiveTab('examens-disponibles');
  };

  const consulterResultatExamen = async (examen) => {
    try {

      const notesVisibles = await verifierNotesVisibles(examen.id);
      
      if (!notesVisibles) {
        setError('Les résultats ne sont pas encore disponibles');
        return;
      }
      const token = localStorage.getItem('token');
      
      // Étape 1: Vérifier l'existence d'une tentative
      const verificationResponse = await fetch(
        `/api/examens/${examen.id}/verifier-tentative`,
        {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      if (verificationResponse.ok) {
        const verificationData = await verificationResponse.json();
        
        if (verificationData.tentativeExistante && verificationData.tentativeId) {
          // Étape 2: Récupérer la tentative complète
          const tentativeResponse = await fetch(
            `/api/tentatives/${verificationData.tentativeId}`,
            {
              method: 'GET',
              headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
              }
            }
          );
          
          if (tentativeResponse.ok) {
            const tentativeData = await tentativeResponse.json();
            
            // Étape 3: Récupérer la note de l'examen (si disponible)
            const noteResponse = await fetch(
              `/api/examens/${examen.id}/ma-note`,
              {
                method: 'GET',
                headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
                }
              }
            );
            
            let noteData = null;
            if (noteResponse.ok) {
              noteData = await noteResponse.json();
            }
            
            // Organiser les données pour l'affichage
            const resultatOrganise = {
              examen: examen,
              tentative: tentativeData,
              note: noteData?.note || tentativeData.noteFinale || 0,
              estCorrigee: tentativeData.estCorrigee || false,
              commentaires: []
            };
            
            // Extraire les commentaires des réponses corrigées
            if (tentativeData.reponses && tentativeData.reponses.length > 0) {
              tentativeData.reponses.forEach((reponse, index) => {
                if (reponse.estCorrigee && reponse.commentaire) {
                  resultatOrganise.commentaires.push({
                    questionNumero: index + 1,
                    questionId: reponse.question?.id || null,
                    texte: reponse.commentaire,
                    pointsObtenus: reponse.notePartielle || 0,
                    pointsMax: reponse.question?.bareme || 0
                  });
                }
              });
            }
            
            setExamenResultat(resultatOrganise.examen);
            setTentativeResultat(resultatOrganise);
            setEtapeExamen('resultat-detail');
          }
        } else {
          setError('Aucune tentative trouvée pour cet examen');
        }
      }
    } catch (err) {
      setError(err.message);
    }
  };

  // Réinitialiser les états de l'examen
  const resetExamen = () => {
    setExamenEnCours(null);
    setTentativeEnCours(null);
    setEtapeExamen(null);
    setReponses({});
    setActiveTab('examens-disponibles');
  };

  // Formater la date
  const formaterDate = (dateString) => {
    if (!dateString) return 'Non défini';
    try {
      return new Date(dateString).toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return 'Date invalide';
    }
  };

  // Formater la durée
  const formaterDuree = (minutes) => {
    if (!minutes) return 'Non spécifié';
    
    const heures = Math.floor(minutes / 60);
    const mins = minutes % 60;
    
    if (heures > 0) {
      return `${heures}h${mins > 0 ? ` ${mins}min` : ''}`;
    }
    return `${mins}min`;
  };

  // Fonctions pour les icônes
  const Icon = ({ type, className = '' }) => {
    const icons = {
      exam: '📝',
      clock: '⏰',
      calendar: '📅',
      check: '✅',
      arrowRight: '➡️',
      eye: '👁️',
      list: '📋',
      close: '❌',
      alert: '⚠️',
      play: '▶️',
      stop: '⏹️',
      plus: '➕',
      trophy: '🏆',
      home: '🏠',
      back: '↩️',
      info: 'ℹ️',
      warning: '⚠️',
      success: '✅',
      error: '❌',
      refresh: '🔄'
    };
    
    return <span className={`icon ${className}`}>{icons[type]}</span>;
  };

  // Effet pour charger les données initiales
  useEffect(() => {
    if (activeTab === 'examens-disponibles' && !etapeExamen && !loading) {
      chargerExamensAvecStatuts();
    } else if (activeTab === 'resultats' && !etapeExamen && !loading) {
      chargerResultats();
    }
  }, [activeTab, etapeExamen]);

  // Effet pour nettoyer les messages
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(''), 3000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  return (
    <div className="etudiant-dashboard">
      <div className="dashboard-header">
        <h1>Tableau de bord - Étudiant</h1>
        
        {successMessage && (
          <div className="alert alert-success">
            <Icon type="success" />
            <span>{successMessage}</span>
          </div>
        )}
        
        {error && (
          <div className="alert alert-error">
            <Icon type="error" />
            <span>{error}</span>
            <button onClick={() => setError(null)} className="close-btn">×</button>
          </div>
        )}
      </div>

      {!etapeExamen && (
        <div className="dashboard-tabs">
          <button 
            className={activeTab === 'examens-disponibles' ? 'tab active' : 'tab'}
            onClick={() => {
              setActiveTab('examens-disponibles');
              chargerExamensAvecStatuts();
            }}
          >
            <Icon type="exam" />
            <span>Examens disponibles</span>
          </button>
          
          
        </div>
      )}

      {etapeExamen === 'resultat-detail' && examenResultat && tentativeResultat && (
  <div className="examen-etape">
    <div className="etape-header">
      <button className="btn-back" onClick={() => {
        setExamenResultat(null);
        setTentativeResultat(null);
        setEtapeExamen(null);
        setActiveTab('examens-disponibles');
      }}>
        <Icon type="back" />
        Retour aux examens
      </button>
    </div>
    
    <div className="resultat-detail">
      <div style={{ padding: '2rem' }}>
        <div className="section-header">
          <h2>Résultats de l'examen : {examenResultat.titre}</h2>
        </div>
        
        {tentativeResultat.estCorrigee ? (
          <div className="resultat-content">
            <div className="score-resume" style={{
              textAlign: 'center',
              padding: '2rem',
              backgroundColor: '#f8f9fa',
              borderRadius: '8px',
              marginBottom: '2rem'
            }}>
              <h3 style={{ marginBottom: '1rem', color: '#333' }}>Votre Score</h3>
              <div style={{ fontSize: '3rem', fontWeight: 'bold', color: '#17a2b8', marginBottom: '0.5rem' }}>
                {tentativeResultat.note?.toFixed(1) || '0.0'}
              </div>
             
            </div>
        
          </div>
        ) : (
          <div className="loading" style={{ textAlign: 'center', padding: '3rem' }}>
            <div className="spinner" style={{
              width: '40px',
              height: '40px',
              border: '4px solid #f3f3f3',
              borderTop: '4px solid #17a2b8',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite',
              margin: '0 auto 1rem'
            }}></div>
            <p style={{ color: '#6c757d' }}>
              {tentativeResultat.tentative?.statut === 'EN_COURS' ? 
                'Examen en cours de correction...' : 
                'En attente de correction...'}
            </p>
            <p className="subtext" style={{ color: '#adb5bd', fontSize: '0.9rem' }}>
              Les résultats seront disponibles une fois l'examen corrigé
            </p>
            <button className="btn-secondary" onClick={() => {
              setExamenResultat(null);
              setTentativeResultat(null);
              setEtapeExamen(null);
              setActiveTab('examens-disponibles');
            }} style={{ marginTop: '1rem' }}>
              <Icon type="back" />
              Retour aux examens
            </button>
          </div>
        )}
      </div>
    </div>
  </div>
)}

      <div className="tab-content">
        {activeTab === 'examens-disponibles' && !etapeExamen && (
          <div className="examens-disponibles">
            <div className="section-header">
              <h2>Mes examens</h2>
              <button 
                className="btn-secondary"
                onClick={chargerExamensAvecStatuts}
                disabled={loading}
              >
                <Icon type="refresh" />
                Rafraîchir
              </button>
            </div>

            {loading ? (
              <div className="loading">
                <div className="spinner"></div>
                <p>Chargement des examens...</p>
              </div>
            ) : examensAvecStatut.length === 0 ? (
              <div className="empty-state">
                <Icon type="exam" className="empty-icon" />
                <p>Aucun examen disponible</p>
                <button 
                  className="btn-secondary"
                  onClick={chargerExamensAvecStatuts}
                >
                  Recharger
                </button>
              </div>
            ) : (
              <div className="examens-grid">
                {examensAvecStatut.map(examen => (
                  <div key={examen.id} className="examen-card">
                    <div className="examen-header">
                      <h3>{examen.titre}</h3>
                      <span className={`statut-badge statut-${examen.badge}`}>
                        {examen.statut}
                      </span>
                    </div>
                    
                    <p className="examen-description">
                      {examen.description || 'Aucune description'}
                    </p>
                    
                    <div className="examen-details">
                      <div className="detail">
                        <Icon type="clock" />
                        <span>Durée: {formaterDuree(examen.dureeMinutes)}</span>
                      </div>
                      <div className="detail">
                        <Icon type="calendar" />
                        <span>Début: {formaterDate(examen.dateDebut)}</span>
                      </div>
                      <div className="detail">
                        <Icon type="calendar" />
                        <span>Fin: {formaterDate(examen.dateFin)}</span>
                      </div>
                      {examen.totalPoints && (
                        <div className="detail">
                          <Icon type="trophy" />
                          <span>Total: {examen.totalPoints} points</span>
                        </div>
                      )}
                    </div>
                    
                    <div className="examen-footer">
                      {examen.disponible ? (
                        <button 
                          className="btn-primary"
                          onClick={() => demarrerOuReprendreExamen(examen)}
                        >
                          <Icon type="play" />
                          {examen.statut === "En cours" ? "Reprendre l'examen" : "Commencer l'examen"}
                        </button>
                      ) : examen.statut === 'Terminé' ? (
                        <button 
                          className="btn-info"
                          onClick={() => consulterResultatExamen(examen)}
                          title="Consulter mes résultats"
                          disabled={!examen.notesVisibles}
                        >
                          <Icon type="eye" />
                          Consulter les résultats
                        </button>
                      ) : (
                        <button 
                          className="btn-secondary"
                          disabled
                          title={examen.statut === 'À venir' ? 'Bientôt disponible' : 'Examen terminé'}
                        >
                          {examen.statut === 'À venir' ? 'Bientôt disponible' : 'Non disponible'}
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'resultats' && !etapeExamen && (
          <div className="resultats">
            <div className="section-header">
              <h2>Mes résultats</h2>
              <button 
                className="btn-secondary"
                onClick={chargerResultats}
                disabled={loading}
              >
                <Icon type="refresh" />
                Rafraîchir
              </button>
            </div>

            {loading ? (
              <div className="loading">
                <div className="spinner"></div>
                <p>Chargement des résultats...</p>
              </div>
            ) : resultats.length === 0 ? (
              <div className="empty-state">
                <Icon type="trophy" className="empty-icon" />
                <p>Aucun résultat disponible</p>
                <p className="subtext">
                  Les résultats apparaîtront ici après la soumission et la correction de vos examens.
                </p>
              </div>
            ) : (
              <div className="resultats-table">
                <table>
                  <thead>
                    <tr>
                      <th>Examen</th>
                      <th>Date</th>
                      <th>Score</th>
                      <th>Note</th>
                      <th>Statut</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resultats.map(resultat => {
                      const pourcentage = resultat.examen?.totalPoints 
                        ? (resultat.score / resultat.examen.totalPoints) * 100 
                        : 0;
                      
                      let noteBadgeClass = 'note-badge ';
                      if (pourcentage >= 80) noteBadgeClass += 'success';
                      else if (pourcentage >= 60) noteBadgeClass += 'warning';
                      else noteBadgeClass += 'danger';
                      
                      let statutBadgeClass = 'statut-badge ';
                      if (resultat.statut === 'SOUMISE') statutBadgeClass += 'statut-termine';
                      else if (resultat.statut === 'EXPIREE') statutBadgeClass += 'statut-termine';
                      else if (resultat.statut === 'EN_COURS') statutBadgeClass += 'statut-en-cours';
                      else statutBadgeClass += 'statut-inconnu';
                      
                      return (
                        <tr key={resultat.id}>
                          <td>
                            <strong>{resultat.examen?.titre || 'Examen inconnu'}</strong>
                            {resultat.examen?.description && (
                              <div className="examen-info">
                                {resultat.examen.description.substring(0, 50)}...
                              </div>
                            )}
                            {!resultat.examen?.description && (
                              <div className="examen-info">Aucune description</div>
                            )}
                          </td>
                          <td>
                            {formaterDate(resultat.dateSoumission || resultat.dateCreation)}
                          </td>
                          <td>
                            <div className="score-display">
                              <span className="score-value">
                                {resultat.score?.toFixed(1) || '0.0'}
                              </span>
                              <span className="score-max">
                                /{resultat.examen?.totalPoints || '?'}
                              </span>
                            </div>
                          </td>
                          <td>
                            <span className={noteBadgeClass}>
                              {pourcentage.toFixed(1)}%
                            </span>
                          </td>
                          <td>
                            <span className={statutBadgeClass}>
                              {resultat.statut || 'Inconnu'}
                            </span>
                          </td>
                          <td>
                            <button 
                              className="btn-info btn-sm"
                              onClick={async () => {
                                try {
                                  const token = localStorage.getItem('token');
                                  
                                  // Trouver l'examen correspondant
                                  const examen = examensAvecStatut.find(e => e.id === resultat.examen?.id) || resultat.examen;
                                  
                                  if (!examen) {
                                    setError('Examen non trouvé');
                                    return;
                                  }
                                  
                                  // Utiliser la même fonction pour consulter les résultats
                                  await consulterResultatExamen(examen);
                                } catch (err) {
                                  setError(err.message);
                                }
                              }}
                              title={resultat.examen?.notesVisibles ? "Voir les détails" : "Les résultats ne sont pas encore disponibles"}
                              disabled={!resultat.examen?.notesVisibles}
                            >
                              <Icon type="eye" />
                              Détails
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {etapeExamen === 'composition' && examenEnCours && tentativeEnCours && (
          <ExamenComposition
            examen={examenEnCours}
            tentativeId={tentativeEnCours.id}
            onSoumettre={soumettreExamen}
            onQuitter={quitterExamen}
          />
        )}

        {etapeExamen === 'confirmation' && examenEnCours && tentativeEnCours && (
          <div className="examen-etape">
            <SoumissionConfirmee
              examen={examenEnCours}
              tentative={tentativeEnCours}
              onRetour={terminerExamen}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default EtudiantDashboard;