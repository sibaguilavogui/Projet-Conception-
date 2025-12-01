import { useState } from 'react';
import axios from 'axios';
import './Login.css';
import { useAuth } from '../contexts/AuthContext';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const response = await axios.post('http://localhost:8080/auth/login', {
        email: email,
        password: password
      });
      
      // Stocker les données utilisateur et le token
      const { user: userData, token } = response.data;
      login(userData, token);

    } catch (error) {
      console.error('Erreur de connexion:', error);
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
        <div className="input-group">
          <div className="input-row">
            <label htmlFor="email" className="input-label">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
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
                onChange={(e) => setPassword(e.target.value)}
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