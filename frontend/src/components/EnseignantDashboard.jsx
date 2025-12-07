import React, { useState, useEffect } from 'react';
import './EnseignantDashboard.css';

const EnseignantDashboard = () => {
  const [activeTab, setActiveTab] = useState('mes-examens');
  const [modificationMode, setModificationMode] = useState('infos'); // 'infos', 'planifier', 'questions'
  const [examens, setExamens] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [resultatsExamen, setResultatsExamen] = useState(null);
  const [resultatsTentatives, setResultatsTentatives] = useState([]);
  const [correctionEnCours, setCorrectionEnCours] = useState(false);
  const [examenTermine, setExamenTermine] = useState(false);
  const [notesPubliees, setNotesPubliees] = useState(false);
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);
  const [modalAction, setModalAction] = useState(null);
  const [statsCorrection, setStatsCorrection] = useState({
    totalTentatives: 0,
    corrigees: 0,
    aCorriger: 0,
    moyenne: 0,
    meilleureNote: 0,
    pireNote: 0
  });
  
  // États pour la création/modification d'examen
  const [nouvelExamen, setNouvelExamen] = useState({
    titre: '',
    description: '',
    dureeMinutes: 60,
    dateDebut: '',
    dateFin: ''
  });
  
  // États pour la modification
  const [examenEnModification, setExamenEnModification] = useState(null);
  const [planificationData, setPlanificationData] = useState({
    dateDebut: '',
    dateFin: '',
    dureeMinutes: 60
  });
  
  // États pour les questions
  const [questions, setQuestions] = useState([]);
  const [nombreQuestions, setNombreQuestions] = useState(0);
  const [nouvelleQuestion, setNouvelleQuestion] = useState({
    type: 'choix',
    enonce: '',
    bareme: 1,
    options: [{ texte: '', estCorrecte: false }, { texte: '', estCorrecte: false }],
    typeChoix: 'UNIQUE',
    politiqueCorrectionQCM: 'TOUT_OU_RIEN'
  });
  
  // États pour les avertissements
  const [avertissements, setAvertissements] = useState({
    enonce: false,
    bareme: false,
    options: []
  });
  
  // États pour les tentatives
  const [tentatives, setTentatives] = useState([]);
  const [tentativesACorriger, setTentativesACorriger] = useState([]);
  const [examenSelectionne, setExamenSelectionne] = useState(null);
  const [filtreTentatives, setFiltreTentatives] = useState('a-corriger');
  
  // État pour l'examen détaillé
  const [examenDetaille, setExamenDetaille] = useState(null);

  // État pour la modal de correction
  const [showCorrectionModal, setShowCorrectionModal] = useState(false);
  const [tentativeEnCorrection, setTentativeEnCorrection] = useState(null);
  const [questionsTentative, setQuestionsTentative] = useState([]);
  const [notesCorrection, setNotesCorrection] = useState({});
  const [commentairesCorrection, setCommentairesCorrection] = useState({});

  const chargerExamens = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/examens', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        
        const examensAvecQuestions = await Promise.all(
          data.map(async (examen) => {
            try {
              const questionsResponse = await fetch(`http://localhost:8080/examens/${examen.id}/questions`, {
                method: 'GET',
                headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
                }
              });
              
              if (questionsResponse.ok) {
                const questionsData = await questionsResponse.json();
                return {
                  ...examen,
                  nombreQuestions: questionsData.length || 0,
                  questions: questionsData
                };
              } else {
                return {
                  ...examen,
                  nombreQuestions: 0,
                  questions: []
                };
              }
            } catch (error) {
              console.error(`Erreur lors du chargement des questions pour l'examen ${examen.id}:`, error);
              return {
                ...examen,
                nombreQuestions: 0,
                questions: []
              };
            }
          })
        );
        
        setExamens(examensAvecQuestions);
      } else {
        throw new Error('Erreur lors du chargement des examens');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };


  const chargerQuestionsDeveloppement = async (tentativeId) => {
      try {
          const token = localStorage.getItem('token');
          const response = await fetch(`http://localhost:8080/tentatives/${tentativeId}/questions-developpement`, {
              method: 'GET',
              headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
              }
          });
          
          if (response.ok) {
              const data = await response.json();
              setTentativeEnCorrection(data);
              setQuestionsTentative(data.questionsDeveloppement);
              
              // Initialiser les notes et commentaires
              const initialNotes = {};
              const initialCommentaires = {};
              
              data.questionsDeveloppement.forEach(question => {
                  initialNotes[question.id] = question.notePartielle || 0;
                  initialCommentaires[question.id] = question.commentaire || '';
              });
              
              setNotesCorrection(initialNotes);
              setCommentairesCorrection(initialCommentaires);
              setShowCorrectionModal(true);
          } else {
              const errorData = await response.text();
              throw new Error(errorData || 'Erreur lors du chargement des questions');
          }
      } catch (err) {
          setError('Erreur lors du chargement des questions: ' + err.message);
      }
  };

  const soumettreCorrectionDeveloppement = async (tentativeId) => {
      try {
          const token = localStorage.getItem('token');
          
          // Préparer les corrections
          const corrections = questionsTentative.map(question => ({
              reponseId: question.reponseId,
              note: notesCorrection[question.id] || 0,
              commentaire: commentairesCorrection[question.id] || ''
          }));
          
          const response = await fetch(`http://localhost:8080/tentatives/${tentativeId}/corriger-developpement`, {
              method: 'POST',
              headers: {
                  'Authorization': `Bearer ${token}`,
                  'Content-Type': 'application/json'
              },
              body: JSON.stringify({ corrections })
          });
          
          if (response.ok) {
              const data = await response.json();
              setSuccessMessage('Correction enregistrée avec succès');
              setShowCorrectionModal(false);
              
              // Recharger les résultats
              if (resultatsExamen) {
                  await chargerResultatsExamen(resultatsExamen.id);
              }
          } else {
              const errorData = await response.text();
              throw new Error(errorData);
          }
      } catch (err) {
          setError('Erreur lors de l\'enregistrement: ' + err.message);
      }
  };


  const chargerQuestions = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/questions`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const questionsData = await response.json();
        setQuestions(questionsData);
        setNombreQuestions(questionsData.length);
        
        if (examenDetaille && examenDetaille.id === examenId) {
          setExamenDetaille({
            ...examenDetaille,
            questions: questionsData,
            nombreQuestions: questionsData.length
          });
        }
      } else {
        const errorData = await response.text();
        throw new Error(errorData || 'Erreur lors du chargement des questions');
      }
    } catch (err) {
      setError('Erreur lors du chargement des questions: ' + err.message);
    }
  };

  const chargerToutesLesTentatives = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/tentatives`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        const tentativesFormatees = data.map(tentative => ({
          id: tentative.id,
          etudiant: tentative.etudiant ? `${tentative.etudiant.nom} ${tentative.etudiant.prenom}` : 'Inconnu',
          email: tentative.etudiant ? tentative.etudiant.email : 'Non disponible',
          dateSoumission: tentative.dateSoumission,
          statut: tentative.statut,
          score: tentative.noteFinale || 0,
          noteFinale: tentative.noteFinale || 0,
          estCorrigee: tentative.estNoteFinaleCalculee || false,
          estNoteFinaleCalculee: tentative.estNoteFinaleCalculee || false,
          nombreQuestionsRepondues: tentative.nombreQuestionsRepondues || 0,
          nombreQuestionsTotal: examenSelectionne ? (examens.find(e => e.id === examenSelectionne)?.nombreQuestions || 0) : 0
        }));
        setTentatives(tentativesFormatees);
      } else {
        const errorData = await response.text();
        throw new Error(errorData || 'Erreur lors du chargement des tentatives');
      }
    } catch (err) {
      setError('Erreur lors du chargement des tentatives: ' + err.message);
    }
  };

  const chargerTentativesACorriger = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/tentatives-a-corriger`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setTentativesACorriger(data);
        setExamenSelectionne(examenId);
      }
    } catch (err) {
      setError('Erreur lors du chargement des tentatives à corriger');
    }
  };

  const chargerTentativesSelonFiltre = async (examenId) => {
    if (filtreTentatives === 'a-corriger') {
      await chargerTentativesACorriger(examenId);
    } else {
      await chargerToutesLesTentatives(examenId);
    }
  };

  const creerExamen = async (e) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/examens', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(nouvelExamen)
      });
      
      if (response.ok) {
        const result = await response.json();
        setSuccessMessage('Examen créé avec succès !');
        setNouvelExamen({
          titre: '',
          description: '',
          dureeMinutes: 60,
          dateDebut: '',
          dateFin: ''
        });
        chargerExamens();
        setActiveTab('mes-examens');
      } else {
        const errorData = await response.text();
        throw new Error(errorData || 'Erreur lors de la création');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const modifierExamen = async (examenId, donnees) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(donnees)
      });
      
      if (response.ok) {
        setSuccessMessage('Examen modifié avec succès !');
        chargerExamens();
        setExamenDetaille(null);
      } else {
        const errorData = await response.text();
        throw new Error(errorData || 'Erreur lors de la modification');
      }
    } catch (err) {
      setError(err.message);
    }
  };

  const supprimerExamen = async (examenId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer cet examen ? Cette action est irréversible.')) {
      return;
    }
    
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        setSuccessMessage('Examen supprimé avec succès');
        chargerExamens();
        setExamenDetaille(null);
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError(err.message);
    }
  };

  const planifierExamen = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/planifier`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(planificationData)
      });
      
      if (response.ok) {
        setSuccessMessage('Examen planifié avec succès !');
        chargerExamens();
        setExamenDetaille(null);
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError(err.message);
    }
  };

  const ajouterQuestion = async (examenId) => {
    setAvertissements({
      enonce: false,
      bareme: false,
      options: []
    });

    if (!nouvelleQuestion.enonce || nouvelleQuestion.enonce.trim().length === 0) {
      setAvertissements(prev => ({...prev, enonce: true}));
      setError('L\'énoncé de la question est obligatoire');
      return;
    }

    if (nouvelleQuestion.bareme <= 0) {
      setAvertissements(prev => ({...prev, bareme: true}));
      setError('Le barème doit être supérieur à 0');
      return;
    }

    if (nouvelleQuestion.type === 'choix') {
      const optionsValides = nouvelleQuestion.options.filter(opt => 
        opt.texte && opt.texte.trim().length > 0
      );
      
      if (optionsValides.length < 2) {
        const newOptionsWarnings = nouvelleQuestion.options.map(opt => 
          !opt.texte || opt.texte.trim().length === 0
        );
        setAvertissements(prev => ({...prev, options: newOptionsWarnings}));
        setError('Toutes les options doivent avoir du texte (minimum 2 options)');
        return;
      }

      if (nouvelleQuestion.options.length < 2) {
        setError('Une question à choix doit avoir au moins 2 options');
        return;
      }
      
      const optionsAvecTexte = nouvelleQuestion.options.filter(opt => 
        opt.texte && opt.texte.trim().length > 0
      );
      if (optionsAvecTexte.length < 2) {
        setError('Au moins 2 options doivent avoir du texte');
        return;
      }
      
      const nbOptionsCorrectes = nouvelleQuestion.options.filter(opt => opt.estCorrecte).length;
      
      if (nouvelleQuestion.typeChoix === 'UNIQUE') {
        if (nbOptionsCorrectes !== 1) {
          setError('Une question à choix unique doit avoir exactement une réponse correcte');
          return;
        }
      } else {
        if (nbOptionsCorrectes < 1) {
          setError('Une question à choix multiple doit avoir au moins 1 réponse correcte');
          return;
        }
      }
    }

    try {
      const token = localStorage.getItem('token');
      const endpoint = nouvelleQuestion.type === 'choix' 
        ? `http://localhost:8080/examens/${examenId}/question-choix`
        : `http://localhost:8080/examens/${examenId}/question-developpement`;
      
      const body = nouvelleQuestion.type === 'choix' ? {
        enonce: nouvelleQuestion.enonce,
        bareme: nouvelleQuestion.bareme,
        options: nouvelleQuestion.options,
        typeChoix: nouvelleQuestion.typeChoix,
        politiqueCorrectionQCM: nouvelleQuestion.politiqueCorrectionQCM
      } : {
        enonce: nouvelleQuestion.enonce,
        bareme: nouvelleQuestion.bareme
      };
      
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });
      
      if (response.ok) {
        const result = await response.json();
        setSuccessMessage('Question ajoutée avec succès !');
        setNouvelleQuestion({
          type: 'choix',
          enonce: '',
          bareme: 1,
          options: [{ texte: '', estCorrecte: false }, { texte: '', estCorrecte: false }],
          typeChoix: 'UNIQUE',
          politiqueCorrectionQCM: 'TOUT_OU_RIEN'
        });
        setAvertissements({
          enonce: false,
          bareme: false,
          options: []
        });
        
        await chargerQuestions(examenId);
        
        setExamens(prev => prev.map(examen => {
          if (examen.id === examenId) {
            return {
              ...examen,
              nombreQuestions: (examen.nombreQuestions || 0) + 1
            };
          }
          return examen;
        }));
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError(err.message);
    }
  };

  const supprimerQuestion = async (examenId, questionId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer cette question ?')) return;
    
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/questions/${questionId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        setSuccessMessage('Question supprimée avec succès !');
        
        await chargerQuestions(examenId);
        
        setExamens(prev => prev.map(examen => {
          if (examen.id === examenId) {
            return {
              ...examen,
              nombreQuestions: Math.max((examen.nombreQuestions || 0) - 1, 0)
            };
          }
          return examen;
        }));
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError(err.message);
    }
  };

  const rendreExamenPret = async (examenId) => {
    if (!window.confirm('Marquer cet examen comme PRÊT ?\nIl sera ouvert automatiquement à la date de début.')) {
        return;
    }
    
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`http://localhost:8080/examens/${examenId}/marquer-pret`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            const result = await response.json();
            setSuccessMessage(result.message);
            chargerExamens();
        } else {
            const errorData = await response.text();
            throw new Error(errorData);
        }
    } catch (err) {
        setError(err.message);
    }
  };

  const formaterDate = (dateString) => {
    if (!dateString) return 'Non défini';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
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

  const ajouterOption = () => {
    setNouvelleQuestion({
      ...nouvelleQuestion,
      options: [...nouvelleQuestion.options, { texte: '', estCorrecte: false }]
    });
  };

  const modifierOption = (index, champ, valeur) => {
  const nouvellesOptions = [...nouvelleQuestion.options];
  
  if (champ === 'estCorrecte' && valeur === true && nouvelleQuestion.typeChoix === 'UNIQUE') {
    nouvellesOptions.forEach((option, i) => {
      if (i !== index) {
        option.estCorrecte = false;
      }
    });
  }
  
  nouvellesOptions[index][champ] = valeur;
  setNouvelleQuestion({ ...nouvelleQuestion, options: nouvellesOptions });
};

  const supprimerOption = (index) => {
    if (nouvelleQuestion.options.length <= 2) {
      setError('Une question doit avoir au moins 2 options');
      return;
    }
    const nouvellesOptions = nouvelleQuestion.options.filter((_, i) => i !== index);
    setNouvelleQuestion({ ...nouvelleQuestion, options: nouvellesOptions });
  };

  const ouvrirModificationExamen = async (examen, mode = 'infos') => {
    setExamenEnModification(examen);
    setModificationMode(mode);
    
    if (mode === 'planifier') {
      setPlanificationData({
        dateDebut: examen.dateDebut ? examen.dateDebut.replace('T', ' ').substring(0, 16) : '',
        dateFin: examen.dateFin ? examen.dateFin.replace('T', ' ').substring(0, 16) : '',
        dureeMinutes: examen.dureeMinutes || 60
      });
    }
    
    if (mode === 'questions') {
      await chargerQuestions(examen.id);
    }
    
    setActiveTab('modifier-examen');
  };

  const afficherDetailsExamen = async (examen) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examen.id}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const examenDetail = await response.json();
        
        const questionsResponse = await fetch(`http://localhost:8080/examens/${examen.id}/questions`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        
        let questionsData = [];
        if (questionsResponse.ok) {
          questionsData = await questionsResponse.json();
        }
        
        setExamenDetaille({
          ...examenDetail,
          questions: questionsData,
          nombreQuestions: questionsData.length
        });
      } else {
        setExamenDetaille({
          ...examen,
          nombreQuestions: examen.nombreQuestions || 0,
          questions: examen.questions || []
        });
      }
    } catch (err) {
      console.error('Erreur lors du chargement des détails:', err);
      setExamenDetaille({
        ...examen,
        nombreQuestions: examen.nombreQuestions || 0,
        questions: examen.questions || []
      });
    }
  };

  const fermerDetailsExamen = () => {
    setExamenDetaille(null);
  };

  const corrigerTentative = async (tentativeId) => {
      try {
          await chargerQuestionsDeveloppement(tentativeId);
      } catch (err) {
          setError('Erreur lors de la correction: ' + err.message);
      }
  };

  const corrigerQuestionDeveloppement = async (questionId, note, commentaire) => {
    try {
      const token = localStorage.getItem('token');
      
      // Trouver la réponse ID pour cette question
      const question = tentativeEnCorrection.questions?.find(q => q.id === questionId);
      if (!question || !question.reponse) {
        setError('Réponse non trouvée pour cette question');
        return;
      }
      
      const response = await fetch(`http://localhost:8080/tentatives/${tentativeEnCorrection.id}/reponses/${question.reponse.id}/corriger-developpement`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          note: note,
          commentaire: commentaire
        })
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccessMessage('Question corrigée avec succès');
        
        // Mettre à jour les notes et commentaires locaux
        setNotesCorrection(prev => ({
          ...prev,
          [questionId]: note
        }));
        setCommentairesCorrection(prev => ({
          ...prev,
          [questionId]: commentaire
        }));
        
        // Recharger la tentative pour voir les changements
        await corrigerTentative(tentativeEnCorrection.id);
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError('Erreur lors de la correction: ' + err.message);
    }
  };

  const verifierExamenTermine = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/est-termine`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setExamenTermine(data.termine);
        return data.termine;
      }
      return false;
    } catch (err) {
      setError('Erreur lors de la vérification: ' + err.message);
      return false;
    }
  };

  const chargerResultatsExamen = async (examenId) => {
    setLoading(true);
    try {
      // Vérifier si l'examen est terminé
      const termine = await verifierExamenTermine(examenId);
      if (!termine) {
        setError('La correction et les résultats ne sont disponibles qu\'après la date de fin de l\'examen.');
        setLoading(false);
        return;
      }

      const token = localStorage.getItem('token');
      
      // Charger l'examen
      const examenResponse = await fetch(`http://localhost:8080/examens/${examenId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (examenResponse.ok) {
        const examenData = await examenResponse.json();
        setResultatsExamen(examenData);
        setNotesPubliees(examenData.notesVisibles || false);
        
        // Charger toutes les tentatives
        await chargerToutesLesTentatives(examenId);
        
        // Charger les statistiques
        await chargerStatsCorrection(examenId);
      }
    } catch (err) {
      setError('Erreur lors du chargement des résultats: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const chargerStatsCorrection = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/tentatives`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const tentatives = await response.json();
        
        const stats = {
          totalTentatives: tentatives.length,
          corrigees: tentatives.filter(t => t.estNoteFinaleCalculee).length,
          aCorriger: tentatives.filter(t => !t.estNoteFinaleCalculee).length,
          moyenne: 0,
          meilleureNote: 0,
          pireNote: 0
        };
        
        const notes = tentatives
          .filter(t => t.noteFinale !== null && t.noteFinale !== undefined && t.estNoteFinaleCalculee)
          .map(t => t.noteFinale);
        
        if (notes.length > 0) {
          stats.moyenne = notes.reduce((a, b) => a + b, 0) / notes.length;
          stats.meilleureNote = Math.max(...notes);
          stats.pireNote = Math.min(...notes);
        }
        
        setStatsCorrection(stats);
      }
    } catch (err) {
      console.error('Erreur lors du chargement des statistiques:', err);
      // Fallback aux valeurs par défaut
      setStatsCorrection({
        totalTentatives: 0,
        corrigees: 0,
        aCorriger: 0,
        moyenne: 0,
        meilleureNote: 0,
        pireNote: 0
      });
    }
  };

  const corrigerAutomatiquement = async (examenId) => {
    setCorrectionEnCours(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/corriger-automatiquement`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const result = await response.json();
        setSuccessMessage('Correction automatique terminée avec succès !');
        // Recharger les résultats
        await chargerResultatsExamen(examenId);
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError('Erreur lors de la correction automatique: ' + err.message);
    } finally {
      setCorrectionEnCours(false);
    }
  };

  const calculerNotesFinales = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/calculer-notes-finales`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        setSuccessMessage('Notes finales calculées avec succès !');
        await chargerResultatsExamen(examenId);
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError('Erreur lors du calcul des notes: ' + err.message);
    }
  };

  const publierNotes = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/publier-notes`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccessMessage('Notes publiées avec succès ! Les étudiants peuvent maintenant voir leurs résultats.');
        setNotesPubliees(true);
        setResultatsExamen(prev => ({ ...prev, notesVisibles: true }));
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError('Erreur lors de la publication des notes: ' + err.message);
    }
  };

  const masquerNotes = async (examenId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/examens/${examenId}/masquer-notes`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccessMessage('Notes masquées avec succès !');
        setNotesPubliees(false);
        setResultatsExamen(prev => ({ ...prev, notesVisibles: false }));
      } else {
        const errorData = await response.text();
        throw new Error(errorData);
      }
    } catch (err) {
      setError('Erreur lors du masquage des notes: ' + err.message);
    }
  };

  const confirmerAction = (action, examenId) => {
    setModalAction({ type: action, examenId });
    setShowConfirmationModal(true);
  };

  const executerAction = async () => {
    if (!modalAction) return;
    
    switch (modalAction.type) {
      case 'corriger_auto':
        await corrigerAutomatiquement(modalAction.examenId);
        break;
      case 'calculer_notes':
        await calculerNotesFinales(modalAction.examenId);
        break;
      case 'publier_notes':
        await publierNotes(modalAction.examenId);
        break;
      case 'masquer_notes':
        await masquerNotes(modalAction.examenId);
        break;
    }
    
    setShowConfirmationModal(false);
    setModalAction(null);
  };

  // Ajoutez ce composant modal pour les confirmations
  const ConfirmationModal = () => {
    if (!showConfirmationModal || !modalAction) return null;
    
    const messages = {
      'corriger_auto': {
        title: 'Lancer la correction automatique',
        message: 'Êtes-vous sûr de vouloir lancer la correction automatique ? Cette action corrigera toutes les questions à choix des tentatives soumises.',
        icon: 'refresh'
      },
      'calculer_notes': {
        title: 'Calculer les notes finales',
        message: 'Cette action calculera les notes finales pour toutes les tentatives. Assurez-vous que toutes les questions à développement ont été corrigées.',
        icon: 'calculator'
      },
      'publier_notes': {
        title: 'Publier les notes',
        message: 'Les notes seront visibles par les étudiants. Êtes-vous sûr de vouloir publier les notes ?',
        icon: 'eye'
      },
      'masquer_notes': {
        title: 'Masquer les notes',
        message: 'Les notes ne seront plus visibles par les étudiants. Souhaitez-vous continuer ?',
        icon: 'eye-off'
      }
    };
    
    const currentMessage = messages[modalAction.type];
    
    return (
      <div className="modal-overlay">
        <div className="modal-content">
          <div className="modal-header">
            <h3>{currentMessage.title}</h3>
            <button 
              className="modal-close"
              onClick={() => setShowConfirmationModal(false)}
            >
              ×
            </button>
          </div>
          <div className="modal-body">
            <div className="confirmation-message">
              <Icon type={currentMessage.icon} />
              <p>{currentMessage.message}</p>
            </div>
          </div>
          <div className="modal-footer">
            <button 
              className="btn-secondary"
              onClick={() => setShowConfirmationModal(false)}
            >
              Annuler
            </button>
            <button 
              className="btn-primary"
              onClick={executerAction}
            >
              Confirmer
            </button>
          </div>
        </div>
      </div>
    );
  };

  const CorrectionModal = () => {
      if (!showCorrectionModal || !tentativeEnCorrection || !questionsTentative) return null;

      // Vérifier si l'examen est terminé
      const examenTermine = tentativeEnCorrection.examenTermine || 
                          (tentativeEnCorrection.dateFin && 
                            new Date(tentativeEnCorrection.dateFin) < new Date());

      if (!examenTermine) {
          return (
              <div className="modal-overlay">
                  <div className="modal-content">
                      <div className="modal-header">
                          <h3>Correction non disponible</h3>
                          <button 
                              className="modal-close"
                              onClick={() => setShowCorrectionModal(false)}
                          >
                              ×
                          </button>
                      </div>
                      <div className="modal-body">
                          <div className="info-banner warning">
                              <Icon type="alert" />
                              <div>
                                  <p>La correction n'est disponible qu'après la date de fin de l'examen.</p>
                                  <p>Date de fin : {formaterDate(tentativeEnCorrection.dateFin)}</p>
                              </div>
                          </div>
                      </div>
                      <div className="modal-footer">
                          <button 
                              className="btn-secondary"
                              onClick={() => setShowCorrectionModal(false)}
                          >
                              Fermer
                          </button>
                      </div>
                  </div>
              </div>
          );
      }

      return (
          <div className="modal-overlay">
              <div className="modal-content" style={{ maxWidth: '900px', maxHeight: '90vh' }}>
                  <div className="modal-header">
                      <h3>Correction des questions à développement</h3>
                      <button 
                          className="modal-close"
                          onClick={() => setShowCorrectionModal(false)}
                      >
                          ×
                      </button>
                  </div>
                  <div className="modal-body">
                      <div className="tentative-info">
                          <p><strong>Étudiant:</strong> {tentativeEnCorrection.etudiant?.nom} {tentativeEnCorrection.etudiant?.prenom}</p>
                          <p><strong>Email:</strong> {tentativeEnCorrection.etudiant?.email}</p>
                          <p><strong>Date de fin de l'examen:</strong> {formaterDate(tentativeEnCorrection.dateFin)}</p>
                      </div>
                      
                      <div className="info-banner">
                          <Icon type="info" />
                          <p>Seules les questions à développement nécessitent une correction manuelle. Les questions à choix sont corrigées automatiquement.</p>
                      </div>
                      
                      <div className="questions-list">
                          {questionsTentative.map((question, index) => (
                              <div key={question.id} className="question-card">
                                  <div className="question-header">
                                      <span className="question-number">Question {question.ordre}</span>
                                      <span className="question-type">Développement</span>
                                      <span className="question-points">{question.bareme} point(s)</span>
                                  </div>
                                  <p className="question-enonce">{question.enonce}</p>
                                  
                                  <div className="reponse-section">
                                      <p><strong>Réponse de l'étudiant:</strong></p>
                                      <div className="reponse-etudiant">
                                          {question.contenu || 'Aucune réponse'}
                                      </div>
                                      
                                      <div className="correction-form">
                                          <div className="form-group">
                                              <label>Note (sur {question.bareme}):</label>
                                              <input
                                                  type="number"
                                                  min="0"
                                                  max={question.bareme}
                                                  step="0.5"
                                                  value={notesCorrection[question.id] || 0}
                                                  onChange={(e) => setNotesCorrection(prev => ({
                                                      ...prev,
                                                      [question.id]: parseFloat(e.target.value) || 0
                                                  }))}
                                                  className="note-input"
                                                  disabled={question.estCorrigee}
                                              />
                                              {question.estCorrigee && (
                                                  <span className="badge-success" style={{ marginLeft: '10px' }}>
                                                      <Icon type="check" />
                                                      Déjà corrigée
                                                  </span>
                                              )}
                                          </div>
                                          <div className="form-group">
                                              <label>Commentaire:</label>
                                              <textarea
                                                  rows="3"
                                                  value={commentairesCorrection[question.id] || ''}
                                                  onChange={(e) => setCommentairesCorrection(prev => ({
                                                      ...prev,
                                                      [question.id]: e.target.value
                                                  }))}
                                                  className="commentaire-input"
                                                  placeholder="Ajouter un commentaire..."
                                                  disabled={question.estCorrigee}
                                              />
                                          </div>
                                      </div>
                                  </div>
                              </div>
                          ))}
                      </div>
                  </div>
                  <div className="modal-footer">
                      <button 
                          className="btn-secondary"
                          onClick={() => setShowCorrectionModal(false)}
                      >
                          Annuler
                      </button>
                      <button 
                          className="btn-success"
                          onClick={() => soumettreCorrectionDeveloppement(tentativeEnCorrection.tentativeId)}
                      >
                          Enregistrer les corrections
                      </button>
                  </div>
              </div>
          </div>
      );
  };

  // Fonctions pour les icônes
  const Icon = ({ type, className = '' }) => {
    const icons = {
      plus: '➕',
      edit: '✏️',
      trash: '🗑️',
      calendar: '📅',
      eye: '👁️',
      check: '✅',
      clock: '⏰',
      users: '👥',
      file: '📄',
      alert: '⚠️',
      settings: '⚙️',
      list: '📋',
      arrowRight: '➡️',
      close: '❌',
      question: '❓',
      refresh: '🔄',
      filter: '🔍',
      download: '📥',
      star: '⭐',
      'eye-off': '👁️‍🗨️'
    };
    
    return <span className={`icon ${className}`}>{icons[type]}</span>;
  };

  useEffect(() => {
    if (activeTab === 'mes-examens') {
      chargerExamens();
    }
  }, [activeTab]);

  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(''), 3000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  return (
    <div className="enseignant-dashboard">
      <div className="dashboard-header">
        <h1>Tableau de bord - Enseignant</h1>
        {successMessage && (
          <div className="alert alert-success">
            <Icon type="check" />
            <span>{successMessage}</span>
          </div>
        )}
        {error && (
          <div className="alert alert-error">
            <Icon type="alert" />
            <span>{error}</span>
            <button onClick={() => setError(null)} className="close-btn">×</button>
          </div>
        )}
      </div>

      <div className="dashboard-tabs">
        <button 
          className={activeTab === 'mes-examens' ? 'tab active' : 'tab'}
          onClick={() => {
            setActiveTab('mes-examens');
            setExamenDetaille(null);
          }}
        >
          <Icon type="file" />
          <span>Mes examens</span>
        </button>
        <button 
          className={activeTab === 'creer-examen' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('creer-examen')}
        >
          <Icon type="plus" />
          <span>Créer un examen</span>
        </button>
        <button 
          className={activeTab === 'corriger' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('corriger')}
        >
          <Icon type="check" />
          <span>À corriger</span>
        </button>
        <button 
          className={activeTab === 'resultats-correction' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('resultats-correction')}
        >
          <Icon type="star" />
          <span>Résultats & correction</span>
        </button>
      </div>

      <div className="tab-content">
        
        {activeTab === 'resultats-correction' && (
          <div className="resultats-correction">
            <div className="section-header">
              <h2>Résultats et correction des examens</h2>
              <div className="filter-section">
                <div className="filter-row">
                  <select 
                    value={resultatsExamen?.id || ''}
                    onChange={async (e) => {
                      const examenId = e.target.value;
                      if (examenId) {
                        await chargerResultatsExamen(examenId);
                      } else {
                        setResultatsExamen(null);
                        setExamenTermine(false);
                      }
                    }}
                    className="examen-select"
                  >
                    <option value="">Sélectionnez un examen</option>
                    {examens.map(examen => (
                      <option key={examen.id} value={examen.id}>
                        {examen.titre} - {examen.etat || 'BROUILLON'}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {loading && (
              <div className="loading">
                <div className="spinner"></div>
                <p>Chargement des résultats...</p>
              </div>
            )}

            {resultatsExamen && (
              <>
                {!examenTermine ? (
                  <div className="info-banner warning">
                    <Icon type="alert" />
                    <div>
                      <strong>Examen non terminé</strong>
                      <p>
                        La correction et les résultats ne sont disponibles qu'après la date de fin de l'examen. 
                        Date de fin : {formaterDate(resultatsExamen.dateFin)}
                      </p>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="examen-info-card">
                      <h3>{resultatsExamen.titre}</h3>
                      <p>{resultatsExamen.description}</p>
                      
                      <div className="examen-info-row">
                        <div className="examen-info-item">
                          <span className="examen-info-label">Date de fin</span>
                          <span className="examen-info-value">{formaterDate(resultatsExamen.dateFin)}</span>
                        </div>
                        <div className="examen-info-item">
                          <span className="examen-info-label">Statut</span>
                          <span className="examen-info-value">
                            <span className={`statut-badge statut-${resultatsExamen.etat?.toLowerCase()}`}>
                              {resultatsExamen.etat}
                            </span>
                          </span>
                        </div>
                        <div className="examen-info-item">
                          <span className="examen-info-label">Notes visibles</span>
                          <span className="examen-info-value">
                            <span className={`toggle-status ${notesPubliees ? 'status-published' : 'status-hidden'}`}>
                              {notesPubliees ? 'Publiées' : 'Masquées'}
                            </span>
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="correction-stats">
                      <div className="stat-card">
                        <div className="stat-label">Tentatives totales</div>
                        <div className="stat-value">{statsCorrection.totalTentatives}</div>
                      </div>
                      <div className="stat-card success">
                        <div className="stat-label">Tentatives corrigées</div>
                        <div className="stat-value">{statsCorrection.corrigees}</div>
                      </div>
                      <div className="stat-card warning">
                        <div className="stat-label">À corriger</div>
                        <div className="stat-value">{statsCorrection.aCorriger}</div>
                      </div>
                      <div className="stat-card">
                        <div className="stat-label">Moyenne générale</div>
                        <div className="stat-value">{statsCorrection.moyenne.toFixed(2)}</div>
                      </div>
                    </div>

                    <div className="action-buttons-row">
                      <button 
                        className="btn-success"
                        onClick={() => confirmerAction('corriger_auto', resultatsExamen.id)}
                        disabled={correctionEnCours}
                      >
                        <Icon type="refresh" />
                        {correctionEnCours ? 'Correction en cours...' : 'Correction automatique'}
                      </button>
                      
                      <button 
                        className="btn-primary"
                        onClick={() => confirmerAction('calculer_notes', resultatsExamen.id)}
                        disabled={correctionEnCours}
                      >
                        <Icon type="calculator" />
                        Calculer notes finales
                      </button>
                      
                      {notesPubliees ? (
                          <button 
                              className="btn-warning"
                              onClick={() => confirmerAction('masquer_notes', resultatsExamen.id)}
                          >
                              <Icon type="eye-off" />
                              Masquer les notes
                          </button>
                      ) : (
                          <button 
                              className="btn-info"
                              onClick={() => confirmerAction('publier_notes', resultatsExamen.id)}
                              disabled={!examenTermine || !resultatsExamen?.dateFin || new Date(resultatsExamen.dateFin) > new Date()}
                              title={!examenTermine ? "La publication n'est disponible qu'après la date de fin de l'examen" : ""}
                          >
                              <Icon type="eye" />
                              {!examenTermine ? "Publication indisponible" : "Publier les notes"}
                          </button>
                      )}
                    </div>

                    <div className="tentatives-table">
                      <div className="table-header">
                        <h3>Tentatives des étudiants ({tentatives.length})</h3>
                        <div className="table-actions">
                          <button 
                            className="btn-secondary btn-sm"
                            onClick={() => resultatsExamen && chargerToutesLesTentatives(resultatsExamen.id)}
                          >
                            <Icon type="refresh" />
                            Rafraîchir
                          </button>
                        </div>
                      </div>
                      
                      {tentatives.length === 0 ? (
                        <div className="empty-state">
                          <Icon type="users" className="empty-icon" />
                          <p>Aucune tentative soumise pour cet examen</p>
                        </div>
                      ) : (
                        <table>
                          <thead>
                            <tr>
                              <th>Étudiant</th>
                              <th>Email</th>
                              <th>Date soumission</th>
                              <th>Statut</th>
                              <th>Note</th>
                              <th>Correction</th>
                              <th>Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {tentatives.map(tentative => (
                              <tr key={tentative.id}>
                                <td>{tentative.etudiant}</td>
                                <td>{tentative.email}</td>
                                <td>{formaterDate(tentative.dateSoumission)}</td>
                                <td>
                                  <span className={`statut-badge statut-${tentative.statut?.toLowerCase()}`}>
                                    {tentative.statut}
                                  </span>
                                </td>
                                <td>
                                  <span className={`score-display ${tentative.noteFinale > 0 ? 'score-positif' : 'score-zero'}`}>
                                    {tentative.estNoteFinaleCalculee ? tentative.noteFinale.toFixed(1) : 'N/A'}
                                    {tentative.estNoteFinaleCalculee && <Icon type="check" className="score-icon" />}
                                  </span>
                                </td>
                                <td>
                                  {tentative.estNoteFinaleCalculee ? (
                                    <span className="badge-success">
                                      <Icon type="check" />
                                      Corrigée
                                    </span>
                                  ) : (
                                    <span className="badge-warning">
                                      <Icon type="alert" />
                                      À corriger
                                    </span>
                                  )}
                                </td>
                                <td>
                                  <div className="action-buttons">
                                    <button 
                                      className="btn-primary btn-sm"
                                      onClick={() => corrigerTentative(tentative.id)}
                                    >
                                      <Icon type="edit" />
                                      {tentative.estNoteFinaleCalculee ? 'Voir' : 'Corriger'}
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      )}
                    </div>
                  </>
                )}
              </>
            )}

            {!resultatsExamen && !loading && (
              <div className="empty-state">
                <Icon type="star" className="empty-icon" />
                <p>Sélectionnez un examen pour voir les résultats et gérer la correction</p>
                <p className="subtext">
                  Cette fonctionnalité permet de gérer les résultats après la date de fin d'un examen, 
                  y compris la correction automatique, le calcul des notes et la publication des résultats.
                </p>
              </div>
            )}
          </div>
        )}


        {activeTab === 'mes-examens' && (
          <div className="mes-examens">
            <div className="section-header">
              <h2>Mes examens</h2>
              <div className="header-actions">
                <button 
                  className="btn-secondary"
                  onClick={chargerExamens}
                  disabled={loading}
                >
                  <Icon type="refresh" />
                  Rafraîchir
                </button>
                <button 
                  className="btn-primary"
                  onClick={() => setActiveTab('creer-examen')}
                >
                  <Icon type="plus" />
                  Nouvel examen
                </button>
              </div>
            </div>

            {loading ? (
              <div className="loading">
                <div className="spinner"></div>
                <p>Chargement des examens...</p>
              </div>
            ) : examenDetaille ? (
              <div className="examen-detail-view">
                <div className="detail-header">
                  <button 
                    className="btn-secondary btn-sm"
                    onClick={fermerDetailsExamen}
                  >
                    <Icon type="close" />
                    Retour à la liste
                  </button>
                  <h3>{examenDetaille.titre}</h3>
                  <span className={`statut-badge statut-${examenDetaille.etat?.toLowerCase() || 'brouillon'}`}>
                    {examenDetaille.etat || 'BROUILLON'}
                  </span>
                </div>
                
                <div className="detail-content">
                  <div className="detail-section">
                    <h4>Description</h4>
                    <p>{examenDetaille.description || 'Aucune description'}</p>
                  </div>
                  
                  <div className="detail-grid">
                    <div className="detail-item">
                      <Icon type="clock" />
                      <div>
                        <span className="detail-label">Durée</span>
                        <span className="detail-value">{examenDetaille.dureeMinutes || 0} minutes</span>
                      </div>
                    </div>
                    
                    <div className="detail-item">
                      <Icon type="question" />
                      <div>
                        <span className="detail-label">Nombre de questions</span>
                        <span className="detail-value">{examenDetaille.nombreQuestions || 0}</span>
                      </div>
                    </div>
                    
                    <div className="detail-item">
                      <Icon type="trophy" />
                      <div>
                        <span className="detail-label">Points totaux</span>
                        <span className="detail-value">
                          {examenDetaille.questions?.reduce((sum, q) => sum + (q.bareme || 0), 0) || 0} points
                        </span>
                      </div>
                    </div>
                    
                    {examenDetaille.dateDebut && (
                      <div className="detail-item">
                        <Icon type="calendar" />
                        <div>
                          <span className="detail-label">Date de début</span>
                          <span className="detail-value">{formaterDate(examenDetaille.dateDebut)}</span>
                        </div>
                      </div>
                    )}
                    
                    {examenDetaille.dateFin && (
                      <div className="detail-item">
                        <Icon type="calendar" />
                        <div>
                          <span className="detail-label">Date de fin</span>
                          <span className="detail-value">{formaterDate(examenDetaille.dateFin)}</span>
                        </div>
                      </div>
                    )}
                  </div>
                  
                  <div className="detail-section">
                    <h4>Actions disponibles</h4>
                    <div className="actions-list">
                      
                      {examenDetaille.etat === 'BROUILLON' && (
                        <button 
                          className="action-item btn-secondary"
                          onClick={() => ouvrirModificationExamen(examenDetaille, 'infos')}
                        >
                          <Icon type="edit" />
                          <div>
                            <span className="action-title">Modifier les informations</span>
                            <span className="action-description">Modifier le titre et la description</span>
                          </div>
                          <Icon type="arrowRight" className="action-arrow" />
                        </button>
                      )}
                      
                      
                      {(examenDetaille.etat === 'BROUILLON' || examenDetaille.etat === 'PRET' || examenDetaille.etat === 'OUVERT') && (
                        <button 
                          className="action-item btn-info"
                          onClick={() => ouvrirModificationExamen(examenDetaille, 'planifier')}
                        >
                          <Icon type="calendar" />
                          <div>
                            <span className="action-title">Planifier l'examen</span>
                            <span className="action-description">Définir les dates et durée</span>
                          </div>
                          <Icon type="arrowRight" className="action-arrow" />
                        </button>
                      )}
                      
                      
                      {examenDetaille.etat === 'BROUILLON' && (
                        <button 
                          className="action-item btn-warning"
                          onClick={() => ouvrirModificationExamen(examenDetaille, 'questions')}
                        >
                          <Icon type="list" />
                          <div>
                            <span className="action-title">Gérer les questions</span>
                            <span className="action-description">Ajouter, modifier ou supprimer des questions</span>
                          </div>
                          <Icon type="arrowRight" className="action-arrow" />
                        </button>
                      )}
                      
                      
                      {examenDetaille.etat === 'BROUILLON' && (
                        <button 
                          className="action-item btn-success"
                          onClick={() => rendreExamenPret(examenDetaille.id)}
                        >
                          <Icon type="check" />
                          <div>
                            <span className="action-title">Rendre l'examen prêt</span>
                            <span className="action-description">Préparer l'examen pour les étudiants</span>
                          </div>
                          <Icon type="arrowRight" className="action-arrow" />
                        </button>
                      )}
                  
                      {examenDetaille.etat === 'BROUILLON' && (
                        <button 
                          className="action-item btn-danger"
                          onClick={() => supprimerExamen(examenDetaille.id)}
                        >
                          <Icon type="trash" />
                          <div>
                            <span className="action-title">Supprimer l'examen</span>
                            <span className="action-description">Action irréversible</span>
                          </div>
                          <Icon type="arrowRight" className="action-arrow" />
                        </button>
                      )}
                    </div>
                  </div>
                  
                  {examenDetaille.questions && examenDetaille.questions.length > 0 && (
                    <div className="detail-section">
                      <h4>Questions ({examenDetaille.nombreQuestions || 0})</h4>
                      <div className="questions-preview">
                        {examenDetaille.questions.slice(0, 5).map((question, index) => (
                          <div key={index} className="question-preview">
                            <span className="question-index">Question {index + 1}</span>
                            <span className="question-type">
                              {question.type === 'QCM' || question.typeChoix === 'UNIQUE' ? 'Choix' : 'Développement'}
                            </span>
                            <span className="question-points">{question.bareme} pts</span>
                          </div>
                        ))}
                        {examenDetaille.questions.length > 5 && (
                          <div className="question-preview more-questions">
                            + {examenDetaille.questions.length - 5} autres questions...
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="examens-grid">
                {examens.length === 0 ? (
                  <div className="empty-state">
                    <Icon type="file" className="empty-icon" />
                    <p>Aucun examen créé</p>
                    <button 
                      className="btn-primary"
                      onClick={() => setActiveTab('creer-examen')}
                    >
                      Créer votre premier examen
                    </button>
                  </div>
                ) : (
                  examens.map(examen => (
                    <div 
                      key={examen.id} 
                      className="examen-card clickable"
                      onClick={() => afficherDetailsExamen(examen)}
                    >
                      <div className="examen-header">
                        <h3>{examen.titre}</h3>
                        <span className={`statut-badge statut-${examen.etat?.toLowerCase() || 'brouillon'}`}>
                          {examen.etat || 'BROUILLON'}
                        </span>
                      </div>
                      
                      <p className="examen-description">
                        {examen.description ? (examen.description.length > 100 ? examen.description.substring(0, 100) + '...' : examen.description) : 'Aucune description'}
                      </p>
                      
                      <div className="examen-details">
                        <div className="detail">
                          <Icon type="clock" />
                          <span>{examen.dureeMinutes || 0} min</span>
                        </div>
                        <div className="detail">
                          <Icon type="question" />
                          <span>{examen.nombreQuestions || 0} questions</span>
                        </div>
                        <div className="detail">
                          <Icon type="trophy" />
                          <span>
                            {examen.questions?.reduce((sum, q) => sum + (q.bareme || 0), 0) || 0} points
                          </span>
                        </div>
                        {examen.dateDebut && (
                          <div className="detail">
                            <Icon type="calendar" />
                            <span>Début: {formaterDate(examen.dateDebut)}</span>
                          </div>
                        )}
                        {examen.dateFin && (
                          <div className="detail">
                            <Icon type="calendar" />
                            <span>Fin: {formaterDate(examen.dateFin)}</span>
                          </div>
                        )}
                      </div>

                      <div className="examen-footer">
                        <span className="click-hint">
                          <Icon type="arrowRight" />
                          Cliquer pour voir les détails
                        </span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        )}

        {activeTab === 'creer-examen' && (
          <div className="creer-examen">
            <h2>Créer un nouvel examen</h2>
            <form className="examen-form" onSubmit={creerExamen}>
              <div className="form-group">
                <label>Titre *</label>
                <input
                  type="text"
                  value={nouvelExamen.titre}
                  onChange={(e) => setNouvelExamen({...nouvelExamen, titre: e.target.value})}
                  required
                  placeholder="Titre de l'examen"
                />
              </div>
              
              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={nouvelExamen.description}
                  onChange={(e) => setNouvelExamen({...nouvelExamen, description: e.target.value})}
                  placeholder="Description de l'examen"
                  rows="3"
                />
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>Durée (minutes)</label>
                  <input
                    type="number"
                    value={nouvelExamen.dureeMinutes}
                    onChange={(e) => setNouvelExamen({...nouvelExamen, dureeMinutes: parseInt(e.target.value) || 60})}
                    min="1"
                  />
                </div>
                
                <div className="form-group">
                  <label>Date de début</label>
                  <input
                    type="datetime-local"
                    value={nouvelExamen.dateDebut}
                    onChange={(e) => setNouvelExamen({...nouvelExamen, dateDebut: e.target.value})}
                  />
                </div>
                
                <div className="form-group">
                  <label>Date de fin</label>
                  <input
                    type="datetime-local"
                    value={nouvelExamen.dateFin}
                    onChange={(e) => setNouvelExamen({...nouvelExamen, dateFin: e.target.value})}
                  />
                </div>
              </div>
              
              <div className="form-actions">
                <button 
                  type="button" 
                  className="btn-secondary"
                  onClick={() => setActiveTab('mes-examens')}
                >
                  Annuler
                </button>
                <button 
                  type="submit" 
                  className="btn-primary"
                  disabled={loading}
                >
                  {loading ? 'Création...' : 'Créer l\'examen'}
                </button>
              </div>
            </form>
          </div>
        )}

        {activeTab === 'modifier-examen' && examenEnModification && (
          <div className="modifier-examen">
            <div className="section-header">
              <h2>Modifier l'examen: {examenEnModification.titre}</h2>
              
              <div className="examen-info-summary">
                <span className="info-item">
                  <Icon type="question" />
                  {nombreQuestions || 0} questions
                </span>
                <span className="info-item">
                  <Icon type="clock" />
                  {examenEnModification.dureeMinutes || 0} minutes
                </span>
              </div>
              
              {examenEnModification.etat === 'OUVERT' && (
                <div className="alert alert-warning">
                  <Icon type="alert" />
                  <span>Cet examen est OUVERT aux étudiants. Vous pouvez seulement ajuster la planification.</span>
                </div>
              )}
              
              {examenEnModification.etat === 'FERME' && (
                <div className="alert alert-info">
                  <Icon type="alert" />
                  <span>Cet examen est FERMÉ. Aucune modification n'est autorisée.</span>
                </div>
              )}
              
              <div className="modification-tabs">
                {examenEnModification.etat === 'BROUILLON' && (
                  <button 
                    className={modificationMode === 'infos' ? 'tab active' : 'tab'}
                    onClick={() => setModificationMode('infos')}
                  >
                    <Icon type="edit" />
                    Informations générales
                  </button>
                )}
                
                {(examenEnModification.etat === 'BROUILLON' || examenEnModification.etat === 'OUVERT') && (
                  <button 
                    className={modificationMode === 'planifier' ? 'tab active' : 'tab'}
                    onClick={() => setModificationMode('planifier')}
                  >
                    <Icon type="calendar" />
                    Planifier
                  </button>
                )}
                
                {examenEnModification.etat === 'BROUILLON' && (
                  <button 
                    className={modificationMode === 'questions' ? 'tab active' : 'tab'}
                    onClick={() => setModificationMode('questions')}
                  >
                    <Icon type="list" />
                    Gérer questions ({nombreQuestions || 0})
                  </button>
                )}
              </div>
              
              <button 
                className="btn-secondary"
                onClick={() => {
                  setExamenEnModification(null);
                  setActiveTab('mes-examens');
                }}
              >
                Retour
              </button>
            </div>
            
            {modificationMode === 'infos' && (
              <div className="examen-form-section">
                <h3>Informations générales</h3>
                <form className="examen-form" onSubmit={(e) => {
                  e.preventDefault();
                  modifierExamen(examenEnModification.id, {
                    titre: examenEnModification.titre,
                    description: examenEnModification.description
                  });
                }}>
                  <div className="form-group">
                    <label>Titre *</label>
                    <input
                      type="text"
                      value={examenEnModification.titre}
                      onChange={(e) => setExamenEnModification({...examenEnModification, titre: e.target.value})}
                      required
                    />
                  </div>
                  
                  <div className="form-group">
                    <label>Description</label>
                    <textarea
                      value={examenEnModification.description}
                      onChange={(e) => setExamenEnModification({...examenEnModification, description: e.target.value})}
                      rows="3"
                    />
                  </div>
                  
                  <button type="submit" className="btn-primary">
                    Enregistrer les modifications
                  </button>
                </form>
              </div>
            )}
            
            {modificationMode === 'planifier' && (
              <div className="planifier-section">
                <h3>Planifier l'examen</h3>
                <form className="examen-form" onSubmit={(e) => {
                  e.preventDefault();
                  planifierExamen(examenEnModification.id);
                }}>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Date de début *</label>
                      <input
                        type="datetime-local"
                        value={planificationData.dateDebut}
                        onChange={(e) => setPlanificationData({...planificationData, dateDebut: e.target.value})}
                        required
                      />
                    </div>
                    
                    <div className="form-group">
                      <label>Date de fin *</label>
                      <input
                        type="datetime-local"
                        value={planificationData.dateFin}
                        onChange={(e) => setPlanificationData({...planificationData, dateFin: e.target.value})}
                        required
                      />
                    </div>
                    
                    <div className="form-group">
                      <label>Durée (minutes) *</label>
                      <input
                        type="number"
                        value={planificationData.dureeMinutes}
                        onChange={(e) => setPlanificationData({...planificationData, dureeMinutes: parseInt(e.target.value) || 0})}
                        min="0"
                        required
                      />
                    </div>
                  </div>
                  
                  <div className="form-actions">
                    <button type="submit" className="btn-primary">
                      Planifier l'examen
                    </button>
                  </div>
                </form>
              </div>
            )}
            
            {modificationMode === 'questions' && (
              <div className="gerer-questions">
                <div className="section-header">
                  <h3>Questions de l'examen ({nombreQuestions || 0})</h3>
                  <button 
                    className="btn-success"
                    onClick={() => rendreExamenPret(examenEnModification.id)}
                    disabled={examenEnModification.etat !== 'BROUILLON' || nombreQuestions === 0}
                    title={nombreQuestions === 0 ? "Ajoutez au moins une question avant de rendre l'examen prêt" : ""}
                  >
                    <Icon type="check" />
                    Rendre l'examen prêt
                  </button>
                </div>
                
                {questions.length === 0 ? (
                  <div className="empty-state">
                    <Icon type="question" className="empty-icon" />
                    <p>Aucune question dans cet examen</p>
                    <p className="subtext">
                      Ajoutez des questions ci-dessous. L'examen doit contenir au moins une question pour être rendu prêt.
                    </p>
                  </div>
                ) : (
                  <div className="questions-list">
                    {questions.map((question, index) => (
                      <div key={question.id || index} className="question-card">
                        <div className="question-header">
                          <span className="question-number">Question {index + 1}</span>
                          <span className="question-type">
                            {(question.typeChoix === "QCM" || question.typeChoix === "UNIQUE") ? 
                            "Question à choix" : "Question à développement"}
                          </span>
                          <span className="question-points">{question.bareme} point(s)</span>
                          <button 
                            className="btn-danger btn-sm"
                            onClick={() => supprimerQuestion(examenEnModification.id, question.id)}
                            title="Supprimer"
                          >
                            <Icon type="trash" />
                          </button>
                        </div>
                        <p className="question-enonce">{question.enonce}</p>
                        {question.typeQuestion && (question.typeQuestion === "QCM" || question.typeQuestion === "UNIQUE") && question.options && (
                          <div className="options-section">
                            <span className="options-label">Options :</span>
                            <ul className="options-list">
                              {question.options.map((option, optIndex) => (
                                <li key={optIndex} className={option.estCorrecte ? 'correct' : ''}>
                                  <span className="option-text">{option.texte}</span>
                                  {option.estCorrecte && <Icon type="check" className="option-correct" />}
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
                
                <div className="ajouter-question-section">
                  <h3>Ajouter une nouvelle question</h3>
                  <form className="question-form" onSubmit={(e) => {
                    e.preventDefault();
                    ajouterQuestion(examenEnModification.id);
                  }}>
                    <div className="form-group">
                      <label>Type de question</label>
                      <select
                        value={nouvelleQuestion.type}
                        onChange={(e) => setNouvelleQuestion({...nouvelleQuestion, type: e.target.value})}
                      >
                        <option value="choix">Question à choix</option>
                        <option value="developpement">Question à développement</option>
                      </select>
                    </div>
                    
                    <div className="form-group">
                      <label>Énoncé *</label>
                      <textarea
                        value={nouvelleQuestion.enonce}
                        onChange={(e) => {
                          setNouvelleQuestion({...nouvelleQuestion, enonce: e.target.value});
                          if (e.target.value.trim()) {
                            setAvertissements(prev => ({...prev, enonce: false}));
                          }
                        }}
                        required
                        placeholder="Énoncé de la question"
                        rows="3"
                        className={avertissements.enonce ? 'input-error' : ''}
                      />
                      {avertissements.enonce && (
                        <div className="avertissement">
                          <Icon type="alert" />
                          <span>L'énoncé est requis</span>
                        </div>
                      )}
                    </div>
                    
                    <div className="form-group">
                      <label>Points *</label>
                      <input
                        type="number"
                        value={nouvelleQuestion.bareme}
                        onChange={(e) => {
                          setNouvelleQuestion({...nouvelleQuestion, bareme: parseFloat(e.target.value) || 0});
                          if (parseFloat(e.target.value) > 0) {
                            setAvertissements(prev => ({...prev, bareme: false}));
                          }
                        }}
                        min="0"
                        step="0.5"
                        className={avertissements.bareme ? 'input-error' : ''}
                      />
                      {avertissements.bareme && (
                        <div className="avertissement">
                          <Icon type="alert" />
                          <span>Le barème doit être supérieur à 0</span>
                        </div>
                      )}
                    </div>
                    
                    {nouvelleQuestion.type === 'choix' && (
                      <>
                        <div className="form-row">
                          <div className="form-group">
                            <label>Type de choix</label>
                            <select
                              value={nouvelleQuestion.typeChoix}
                              onChange={(e) => setNouvelleQuestion({...nouvelleQuestion, typeChoix: e.target.value})}
                            >
                              <option value="UNIQUE">Réponse unique</option>
                              <option value="QCM">Choix multiples (QCM)</option>
                            </select>
                          </div>
                          
                          <div className="form-group">
                            <label>Politique de correction</label>
                            <select
                              value={nouvelleQuestion.politiqueCorrectionQCM}
                              onChange={(e) => setNouvelleQuestion({...nouvelleQuestion, politiqueCorrectionQCM: e.target.value})}
                            >
                              <option value="TOUT_OU_RIEN">TOUT OU RIEN</option>
                              <option value="MOYENNE_BONNES">MOYENNE DES BONNES</option>
                              <option value="MOYENNE_BONNES_ET_MAUVAISES">MOYENNE DES BONNES ET MAUVAISES</option>
                            </select>
                          </div>
                        </div>
                        
                        <div className="options-section">
                          <h4>Options de réponse (minimum 2)</h4>
                          {nouvelleQuestion.options.map((option, index) => (
                            <div key={index} className="option-row">
                              <div className="option-input-wrapper">
                                <input
                                  type="text"
                                  value={option.texte}
                                  onChange={(e) => {
                                    modifierOption(index, 'texte', e.target.value);
                                    const newWarnings = [...avertissements.options];
                                    newWarnings[index] = !e.target.value.trim();
                                    setAvertissements(prev => ({...prev, options: newWarnings}));
                                  }}
                                  placeholder={`Option ${index + 1}`}
                                  className={`option-input ${avertissements.options[index] ? 'input-error' : ''}`}
                                />
                                {avertissements.options[index] && (
                                  <div className="avertissement option-avertissement">
                                    <Icon type="alert" />
                                    <span>Texte requis</span>
                                  </div>
                                )}
                              </div>
                              <label className="checkbox-label">
                                <input
                                  type="checkbox"
                                  checked={option.estCorrecte}
                                  onChange={(e) => modifierOption(index, 'estCorrecte', e.target.checked)}
                                />
                                Correcte
                              </label>
                              {nouvelleQuestion.options.length > 2 && (
                                <button
                                  type="button"
                                  className="btn-danger btn-sm"
                                  onClick={() => supprimerOption(index)}
                                  title="Supprimer l'option"
                                >
                                  <Icon type="trash" />
                                </button>
                              )}
                            </div>
                          ))}
                          <div className="avertissement-info">
                            <Icon type="alert" />
                            <span>Attention: Toutes les options doivent avoir du texte</span>
                          </div>
                          <button
                            type="button"
                            className="btn-secondary btn-sm"
                            onClick={ajouterOption}
                          >
                            <Icon type="plus" />
                            Ajouter une option
                          </button>
                        </div>
                      </>
                    )}
                    
                    <div className="form-actions">
                      <button type="submit" className="btn-primary">
                        Ajouter la question
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'corriger' && (
          <div className="corriger-examens">
            <div className="section-header">
              <h2>Tentatives des étudiants</h2>
              <div className="filter-section">
                <div className="filter-row">
                  <select 
                    value={examenSelectionne || ''}
                    onChange={(e) => {
                      const examenId = e.target.value;
                      setExamenSelectionne(examenId);
                      if (examenId) {
                        chargerTentativesSelonFiltre(examenId);
                      }
                    }}
                    className="examen-select"
                  >
                    <option value="">Sélectionnez un examen</option>
                    {examens.map(examen => (
                      <option key={examen.id} value={examen.id}>
                        {examen.titre} ({examen.nombreQuestions || 0} questions)
                      </option>
                    ))}
                  </select>
                  
                  <select 
                    value={filtreTentatives}
                    onChange={(e) => {
                      setFiltreTentatives(e.target.value);
                      if (examenSelectionne) {
                        chargerTentativesSelonFiltre(examenSelectionne);
                      }
                    }}
                    className="filtre-select"
                  >
                    <option value="a-corriger">À corriger</option>
                    <option value="toutes">Toutes les tentatives</option>
                  </select>
                </div>
              </div>
            </div>
            
            {examenSelectionne ? (
              filtreTentatives === 'a-corriger' ? (
                tentativesACorriger.length === 0 ? (
                  <div className="empty-state">
                    <Icon type="check" className="empty-icon" />
                    <p>Aucune tentative à corriger pour cet examen</p>
                    <p className="subtext">
                      Les tentatives avec des questions à développement non corrigées apparaîtront ici.
                    </p>
                  </div>
                ) : (
                  <div className="tentatives-table">
                    <div className="table-header">
                      <h3>Tentatives à corriger ({tentativesACorriger.length})</h3>
                      <div className="table-info">
                        <span>
                          <Icon type="alert" /> Questions à développement nécessitant une correction manuelle
                        </span>
                      </div>
                    </div>
                    <table>
                      <thead>
                        <tr>
                          <th>Étudiant</th>
                          <th>Email</th>
                          <th>Date de soumission</th>
                          <th>Questions à corriger</th>
                          <th>Note actuelle</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {tentativesACorriger.map(tentative => (
                          <tr key={tentative.id}>
                            <td>{tentative.etudiantNom}</td>
                            <td>{tentative.etudiantEmail}</td>
                            <td>{formaterDate(tentative.dateSoumission)}</td>
                            <td>
                              <span className="badge-avertissement">
                                {tentative.questionsDevNonCorrigees} question(s)
                              </span>
                            </td>
                            <td>
                              <span className={tentative.noteFinale > 0 ? 'score-positif' : 'score-zero'}>
                                {tentative.noteFinale || 0}
                              </span>
                            </td>
                            <td>
                              <button 
                                className="btn-primary btn-sm"
                                onClick={() => corrigerTentative(tentative.id)}
                              >
                                <Icon type="edit" />
                                Corriger
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )
              ) : (
                tentatives.length === 0 ? (
                  <div className="empty-state">
                    <Icon type="users" className="empty-icon" />
                    <p>Aucune tentative soumise pour cet examen</p>
                  </div>
                ) : (
                  <div className="tentatives-table">
                    <div className="table-header">
                      <h3>Toutes les tentatives ({tentatives.length})</h3>
                      <div className="table-actions">
                        <button 
                          className="btn-secondary btn-sm"
                          onClick={() => examenSelectionne && chargerToutesLesTentatives(examenSelectionne)}
                        >
                          <Icon type="refresh" />
                          Rafraîchir
                        </button>
                      </div>
                    </div>
                    <table>
                      <thead>
                        <tr>
                          <th>Étudiant</th>
                          <th>Email</th>
                          <th>Date de soumission</th>
                          <th>Statut</th>
                          <th>Note</th>
                          <th>Correction</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {tentatives.map(tentative => (
                          <tr key={tentative.id}>
                            <td>{tentative.etudiant}</td>
                            <td>{tentative.email}</td>
                            <td>{formaterDate(tentative.dateSoumission)}</td>
                            <td>
                              <span className={`statut-badge statut-${tentative.statut?.toLowerCase()}`}>
                                {tentative.statut}
                              </span>
                            </td>
                            <td>
                              <span className={`score-display ${tentative.noteFinale > 0 ? 'score-positif' : 'score-zero'}`}>
                                {tentative.estNoteFinaleCalculee ? tentative.noteFinale.toFixed(1) : 'N/A'}
                                {tentative.estNoteFinaleCalculee && <Icon type="check" className="score-icon" />}
                              </span>
                            </td>
                            <td>
                              {tentative.estNoteFinaleCalculee ? (
                                <span className="badge-success">
                                  <Icon type="check" />
                                  Corrigée
                                </span>
                              ) : (
                                <span className="badge-warning">
                                  <Icon type="alert" />
                                  À corriger
                                </span>
                              )}
                            </td>
                            <td>
                              <div className="action-buttons">
                                <button 
                                  className="btn-primary btn-sm"
                                  onClick={() => corrigerTentative(tentative.id)}
                                >
                                  <Icon type="edit" />
                                  {tentative.estNoteFinaleCalculee ? 'Voir' : 'Corriger'}
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )
              )
            ) : (
              <div className="empty-state">
                <Icon type="users" className="empty-icon" />
                <p>Sélectionnez un examen pour voir les tentatives des étudiants</p>
                <p className="subtext">
                  Choisissez un examen dans la liste déroulante pour afficher les tentatives soumises par les étudiants.
                </p>
              </div>
            )}
          </div>
        )}
      </div>
      <ConfirmationModal />
      <CorrectionModal />
    </div>
  );
};

export default EnseignantDashboard;