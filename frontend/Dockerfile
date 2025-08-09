# Build stage
FROM node:18 as build
WORKDIR /app

# Copier les fichiers de dépendances
COPY package*.json ./
RUN npm install --legacy-peer-deps

# Copier le code source
COPY . .

# Builder l'application avec la configuration Docker
RUN npm run build -- --configuration docker --output-hashing=all --optimization=true --aot=true --build-optimizer=true --vendor-chunk=true --progress=true

# Production stage
FROM nginx:alpine

# Copier les fichiers buildés
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Copier la configuration Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exposer le port 80
EXPOSE 80

# Commande par défaut
CMD ["nginx", "-g", "daemon off;"] 