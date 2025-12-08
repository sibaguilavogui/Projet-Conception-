# Étape de build
FROM node:18-alpine AS builder

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers de dépendances
COPY package*.json ./

# Installer les dépendances
RUN npm ci --only=production

# Copier le code source
COPY . .

# Build l'application
RUN npm run build

# Étape de production
FROM nginx:alpine

# Copier la configuration Nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Copier les fichiers buildés depuis l'étape précédente
COPY --from=builder /app/build /usr/share/nginx/html

# Exposer le port 80
EXPOSE 80

# Lancer Nginx
CMD ["nginx", "-g", "daemon off;"]