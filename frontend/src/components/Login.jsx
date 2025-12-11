import { useState } from 'react';
import axios from 'axios';
import './Login.css';
import { useAuth } from '../contexts/AuthContext';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(''); // État pour l'erreur

  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(''); // Réinitialiser l'erreur à chaque tentative
    setIsLoading(true);
    
    try {
      const response = await axios.post('/api/auth/login', {
        email: email,
        password: password
      });
      
      // Stocker les données utilisateur et le token
      const { user: userData, token } = response.data;
      login(userData, token);

    } catch (error) {
      console.error('Erreur de connexion:', error);
      
      if (error.response) {
        // Le serveur a répondu avec un statut d'erreur
        if (error.response.status === 401) {
          setError('Identifiants invalides. Veuillez réessayer.');
        } else if (error.response.status === 400) {
          setError('Veuillez vérifier les champs saisis.');
        } else {
          setError('Une erreur est survenue. Veuillez réessayer plus tard.');
        }
      } else if (error.request) {
        setError('Impossible de contacter le serveur. Vérifiez votre connexion.');
      } else {
        setError('Erreur de configuration de la requête.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  return (
    <div className="login-container">
      <div className="login-header">
        <h1>EXAM-GU</h1>
      </div>

      <form onSubmit={handleSubmit} className="login-form">
        {/* Afficher l'erreur si elle existe */}
        {error && <div className="error-message">{error}</div>}

        <div className="input-group">
          <div className="input-row">
            <label htmlFor="email" className="input-label">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setError('');
              }}
              className="email-input"
              required
            />
          </div>
          
          <div className="input-row">
            <label htmlFor="password" className="input-label">Mot de passe</label>
            <div className="password-container">
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setError('');
                }}
                className="password-input"
                required
              />
              <button
                type="button"
                className="password-toggle"
                onClick={togglePasswordVisibility}
              >
                {showPassword ? "👁️" : "👁️‍🗨️"}
              </button>
            </div>
          </div>
        </div>

        <div className="button-group">
          <button type="submit" className="btn-next" disabled={isLoading}>
            {isLoading ? "Connexion..." : "Se connecter"}
          </button>
        </div>
      </form>
    </div>
  );
};

export default Login;