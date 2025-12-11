import React, { useState, useEffect, useCallback, useRef } from 'react';
import './ExamenComposition.css';

const ExamenComposition = ({ examen, tentativeId, onSoumettre, onQuitter }) => {
  const [questions, setQuestions] = useState([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [reponses, setReponses] = useState({});
  const [tempsRestant, setTempsRestant] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sauvegardeEnCours, setSauvegardeEnCours] = useState(false);
  
  const timerInterval = useRef(null);
  const sauvegardeInterval = useRef(null);

  // Fonction pour charger les questions et les réponses existantes
  const chargerQuestions = useCallback(async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/tentatives/${tentativeId}/questions`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Erreur lors du chargement des questions');
      }

      const data = await response.json();
      
      if (data.questions && Array.isArray(data.questions)) {
        setQuestions(data.questions);

        // Initialiser les réponses à partir des réponses existantes
        const reponsesExistantes = {};
        data.questions.forEach((question) => {
          if (question.reponseExistante !== null && question.reponseExistante !== undefined) {
            reponsesExistantes[question.id] = question.reponseExistante;
          }
        });
        setReponses(reponsesExistantes);
      }

      if (data.tentative && data.tentative.tempsRestant) {
        setTempsRestant(data.tentative.tempsRestant);
      }

      setLoading(false);
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  }, [tentativeId]);

  // Fonction pour sauvegarder une réponse
  const sauvegarderReponse = useCallback(async (questionId, contenu) => {
    if (sauvegardeEnCours) return;
    
    setSauvegardeEnCours(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/tentatives/${tentativeId}/save-reponse`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ 
          questionId, 
          contenu: contenu || '' 
        })
      });

      if (!response.ok) {
        throw new Error('Erreur lors de la sauvegarde');
      }
    } catch (err) {
      console.error('Erreur de sauvegarde:', err);
    } finally {
      setSauvegardeEnCours(false);
    }
  }, [tentativeId, sauvegardeEnCours]);

  // Fonction pour mettre à jour le temps restant
  const updateTempsRestant = useCallback(async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/tentatives/${tentativeId}/statut-temps-reel`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        const nouveauTempsRestant = data.tempsRestantSecondes || 0;
        setTempsRestant(nouveauTempsRestant);

        if (nouveauTempsRestant <= 0 && !isSubmitting) {
          handleSoumissionAuto();
        }
      }
    } catch (err) {
      console.error('Erreur lors de la mise à jour du temps:', err);
    }
  }, [tentativeId, isSubmitting]);

  // Soumission automatique quand le temps est écoulé
  const handleSoumissionAuto = useCallback(async () => {
    if (isSubmitting) return;
    
    setIsSubmitting(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/tentatives/${tentativeId}/soumettre`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        if (onSoumettre) {
          onSoumettre();
        }
      }
    } catch (err) {
      console.error('Erreur lors de la soumission automatique:', err);
    } finally {
      setIsSubmitting(false);
    }
  }, [tentativeId, onSoumettre, isSubmitting]);

  const handleReponseChange = useCallback((questionId, contenu) => {
    setReponses(prev => ({ ...prev, [questionId]: contenu }));
    
    sauvegarderReponse(questionId, contenu);
  }, [sauvegarderReponse]);

  const allerQuestionPrecedente = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1);
    }
  };

  const allerQuestionSuivante = () => {
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
    }
  };

  const allerQuestion = (index) => {
    if (index >= 0 && index < questions.length) {
      setCurrentQuestionIndex(index);
    }
  };

  const updateTempsRestantRef = useRef();
  const sauvegarderReponseRef = useRef();

  useEffect(() => {
    updateTempsRestantRef.current = updateTempsRestant;
  }, [updateTempsRestant]);

  useEffect(() => {
    sauvegarderReponseRef.current = sauvegarderReponse;
  }, [sauvegarderReponse]);

  useEffect(() => {
    chargerQuestions();
    
    const timerId = setInterval(() => {
      if (updateTempsRestantRef.current) {
        updateTempsRestantRef.current();
      }
    }, 1000);
    
    const saveId = setInterval(() => {
      Object.entries(reponses).forEach(([questionId, contenu]) => {
        if (contenu && contenu.trim() !== '' && sauvegarderReponseRef.current) {
          sauvegarderReponseRef.current(questionId, contenu);
        }
      });
    }, 30000); // 30 secondes

    return () => {
      clearInterval(timerId);
      clearInterval(saveId);
    };
  }, [chargerQuestions]); 
  const formatTemps = (secondes) => {
    if (secondes <= 0) return '00:00';
    
    const heures = Math.floor(secondes / 3600);
    const minutes = Math.floor((secondes % 3600) / 60);
    const secs = secondes % 60;
    
    if (heures > 0) {
      return `${heures.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // Obtenir la couleur du timer selon le temps restant
  const getTimerColor = () => {
    if (tempsRestant < 300) return 'timer-rouge'; // Moins de 5 minutes
    if (tempsRestant < 600) return 'timer-orange'; // Moins de 10 minutes
    return 'timer-vert';
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Chargement de l'examen...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <div className="alert alert-error">
          <span>Erreur: {error}</span>
          <button className="btn btn-secondary" onClick={onQuitter}>
            Retour
          </button>
        </div>
      </div>
    );
  }

  if (questions.length === 0) {
    return (
      <div className="empty-exam">
        <p>Aucune question disponible pour cet examen.</p>
        <button className="btn btn-primary" onClick={onQuitter}>
          Retour
        </button>
      </div>
    );
  }

  const currentQuestion = questions[currentQuestionIndex];
  const totalQuestions = questions.length;
  const questionsRepondues = Object.keys(reponses).filter(id => reponses[id] && reponses[id].toString().trim() !== '').length;

  return (
    <div className="examen-composition">
      <div className="composition-header">
        <div className="header-left">
          <button className="btn-back" onClick={onQuitter}>
            <span className="icon">←</span> Quitter
          </button>
          <h2>{examen.titre}</h2>
          <p className="exam-description">{examen.description}</p>
        </div>
        
        <div className="header-right">
          <div className={`timer ${getTimerColor()}`}>
            <div className="timer-label">Temps restant</div>
            <div className="timer-value">{formatTemps(tempsRestant)}</div>
            {tempsRestant < 300 && (
              <div className="timer-warning">Temps critique!</div>
            )}
          </div>
          
          <div className="progress-stats">
            <div className="stat">
              <span className="stat-label">Questions:</span>
              <span className="stat-value">{questionsRepondues}/{totalQuestions}</span>
            </div>
            <div className="progress-bar">
              <div 
                className="progress-fill" 
                style={{ width: `${(questionsRepondues / totalQuestions) * 100}%` }}
              ></div>
            </div>
          </div>
          
          <div className="header-actions">
            <button 
              className="btn btn-warning" 
              onClick={onQuitter}
              disabled={isSubmitting}
            >
              Sauvegarder et quitter
            </button>
            <button 
              className="btn btn-success" 
              onClick={onSoumettre}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Soumission...' : 'Soumettre l\'examen'}
            </button>
          </div>
        </div>
      </div>

      {/* Contenu principal */}
      <div className="composition-main">
        <div className="questions-sidebar">
          <h3>Questions</h3>
          <div className="questions-grid">
            {questions.map((question, index) => {
              const estRepondu = reponses[question.id] && reponses[question.id].toString().trim() !== '';
              const estCourante = index === currentQuestionIndex;
              
              return (
                <button
                  key={question.id}
                  className={`question-num ${estCourante ? 'active' : ''} ${estRepondu ? 'answered' : ''}`}
                  onClick={() => allerQuestion(index)}
                  title={`Question ${index + 1}`}
                >
                  {index + 1}
                </button>
              );
            })}
          </div>
          
          <div className="sidebar-footer">
            <div className="legend">
              <div className="legend-item">
                <div className="legend-color current"></div>
                <span>Question actuelle</span>
              </div>
              <div className="legend-item">
                <div className="legend-color answered"></div>
                <span>Répondu</span>
              </div>
              <div className="legend-item">
                <div className="legend-color"></div>
                <span>Non répondu</span>
              </div>
            </div>
          </div>
        </div>

        {/* Zone de question */}
        <div className="question-zone">
          <div className="question-header">
            <h3>Question {currentQuestionIndex + 1} sur {totalQuestions}</h3>
            <span className="question-points">({currentQuestion.bareme} point(s))</span>
          </div>
          
          <div className="question-content">
            <div className="question-enonce">
              <p>{currentQuestion.enonce}</p>
            </div>
            
            {/* Affichage selon le type de question */}
            {currentQuestion.typeQuestion === 'CHOIX' && (
              <QuestionChoix 
                question={currentQuestion}
                reponse={reponses[currentQuestion.id] || ''}
                onReponseChange={(contenu) => handleReponseChange(currentQuestion.id, contenu)}
              />
            )}
            
            {currentQuestion.typeQuestion === 'DEVELOPPEMENT' && (
              <QuestionDeveloppement 
                reponse={reponses[currentQuestion.id] || ''}
                onReponseChange={(contenu) => handleReponseChange(currentQuestion.id, contenu)}
              />
            )}
          </div>
          
          {/* Navigation entre questions */}
          <div className="question-navigation">
            <button 
              className="btn btn-secondary"
              onClick={allerQuestionPrecedente}
              disabled={currentQuestionIndex === 0}
            >
              ← Question précédente
            </button>
            
            <div className="nav-middle">
              <span>Question {currentQuestionIndex + 1} / {totalQuestions}</span>
            </div>
            
            <button 
              className="btn btn-secondary"
              onClick={allerQuestionSuivante}
              disabled={currentQuestionIndex === totalQuestions - 1}
            >
              Question suivante →
            </button>
          </div>
        </div>
      </div>

      {/* Pied de page avec sauvegarde automatique */}
      <div className="composition-footer">
        <div className="auto-save-status">
          {sauvegardeEnCours ? (
            <span className="saving">Sauvegarde en cours...</span>
          ) : (
            <span className="saved">✓ Dernière sauvegarde: {new Date().toLocaleTimeString()}</span>
          )}
        </div>
        
        <div className="footer-actions">
          <button 
            className="btn btn-info"
            onClick={() => {
              // Sauvegarder toutes les réponses maintenant
              Object.entries(reponses).forEach(([questionId, contenu]) => {
                sauvegarderReponse(questionId, contenu);
              });
            }}
          >
            Sauvegarder maintenant
          </button>
        </div>
      </div>
    </div>
  );
};

// Sous-composant pour les questions à choix
const QuestionChoix = ({ question, reponse, onReponseChange }) => {
  const handleChoixChange = (choixId) => {
    let nouvelleReponse;
    
    if (question.typeChoix === 'MULTIPLE' || question.typeChoix === 'QCM') {
      const reponsesActuelles = reponse ? reponse.split(',').filter(id => id && id.trim() !== '') : [];
      
      const index = reponsesActuelles.indexOf(choixId);
      
      if (index !== -1) {
        reponsesActuelles.splice(index, 1);
        nouvelleReponse = reponsesActuelles.join(',');
      } else {
        const maxChoix = question.nombreChoixMax || reponsesActuelles.length + 1;
        
        if (reponsesActuelles.length < maxChoix) {
          reponsesActuelles.push(choixId);
          nouvelleReponse = reponsesActuelles.join(',');
        } else {
          return;
        }
      }
    } else {
      nouvelleReponse = choixId;
    }
    
    onReponseChange(nouvelleReponse);
  };

  const estCoche = (choixId) => {
    if (!reponse) return false;
    
    if (question.typeChoix === 'MULTIPLE' || question.typeChoix === 'QCM') {
      return reponse.split(',').includes(choixId);
    }
    
    return reponse === choixId;
  };

  return (
    <div className="question-choix">
      <div className="choix-instructions">
        {question.typeChoix === 'UNIQUE' && (
          <p>Sélectionnez une seule réponse.</p>
        )}
        {(question.typeChoix === 'MULTIPLE' || question.typeChoix === 'QCM') && (
          <p>
            Sélectionnez entre {question.nombreChoixMin} et {question.nombreChoixMax} réponses.
            {reponse && reponse.split(',').filter(id => id).length > 0 && (
              <span className="selection-count">
                ({reponse.split(',').filter(id => id).length} sélectionné(s))
              </span>
            )}
          </p>
        )}
      </div>
      
      <div className="choix-list">
        {question.choix && question.choix.map((choix) => (
          <div key={choix.id} className="choix-item">
            <input
              type={question.typeChoix === 'UNIQUE' ? 'radio' : 'checkbox'}
              id={`choix-${choix.id}`}
              name={`question-${question.id}`}
              checked={estCoche(choix.id)}
              onChange={() => handleChoixChange(choix.id)}
              disabled={question.typeChoix === 'MULTIPLE' && 
                       reponse && 
                       reponse.split(',').filter(id => id).length >= question.nombreChoixMax &&
                       !estCoche(choix.id)}
            />
            <label htmlFor={`choix-${choix.id}`}>
              {choix.texte}
            </label>
          </div>
        ))}
      </div>
    </div>
  );
};

// Sous-composant pour les questions à développement
const QuestionDeveloppement = ({ reponse, onReponseChange }) => {
  return (
    <div className="question-developpement">
      <div className="dev-instructions">
        <p>Rédigez votre réponse dans la zone ci-dessous.</p>
      </div>
      
      <textarea
        value={reponse}
        onChange={(e) => onReponseChange(e.target.value)}
        placeholder="Votre réponse ici..."
        rows={12}
        className="dev-textarea"
      />
      
      <div className="dev-info">
        <span className="char-count">
          {reponse ? reponse.length : 0} caractères
        </span>
      </div>
    </div>
  );
};

export default ExamenComposition;