import React, { useState, useEffect } from 'react';
import './SoumissionConfirmee.css';

const SoumissionConfirmee = ({ examen, tentative, onRetour }) => {
  const [score, setScore] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (tentative && tentative.score !== undefined) {
      setScore(tentative.score);
      setLoading(false);
    } else {
      const timer = setTimeout(() => {
        setScore(tentative?.score || 0);
        setLoading(false);
      }, 1500);
      
      return () => clearTimeout(timer);
    }
  }, [tentative]);

  const formaterDate = (dateString) => {
    if (!dateString) return 'Non disponible';
    return new Date(dateString).toLocaleString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const calculerPourcentage = () => {
    if (!score || !examen.totalPoints) return 0;
    return (score / examen.totalPoints) * 100;
  };

  const getNoteBadgeClass = (pourcentage) => {
    if (pourcentage >= 80) return 'note-excellente';
    if (pourcentage >= 60) return 'note-bonne';
    if (pourcentage >= 40) return 'note-moyenne';
    return 'note-faible';
  };

  return (
    <div className="soumission-confirmee">
      <div className="confirmation-header">
        <div className="checkmark">
          <div className="checkmark-circle"></div>
          <div className="checkmark-stem"></div>
          <div className="checkmark-kick"></div>
        </div>
        <h2>Examen soumis avec succès!</h2>
        <p>Votre tentative a été enregistrée et sera corrigée.</p>
      </div>
      
      <div className="confirmation-content">
        <div className="exam-summary">
          <h3>Résumé de l'examen</h3>
          <div className="summary-details">
            <div className="summary-item">
              <span className="summary-label">Examen:</span>
              <span className="summary-value">{examen.titre}</span>
            </div>
            
            <div className="summary-item">
              <span className="summary-label">Date de soumission:</span>
              <span className="summary-value">
                {formaterDate(tentative?.dateSoumission)}
              </span>
            </div>
          </div>
        </div>
        
        <div className="confirmation-actions">
          <button className="btn btn-primary" onClick={onRetour}>
            Retour au tableau de bord
          </button>
        </div>
      </div>
    </div>
  );
};

export default SoumissionConfirmee;