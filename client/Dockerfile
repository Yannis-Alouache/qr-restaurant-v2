# Image du parcours client Angular : build Node puis service statique nginx.
# Le conteneur nginx sert le build et proxifie /api et /ws vers le service
# « api » du même stack (cf. nginx.conf) : le SPA reste same-origin, sans CORS.
# Construction : docker build -t qr-restaurant-client ./client (depuis la racine)

# --- Étape de build -----------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /build

# Cache des dépendances : les manifests seuls suffisent à cette couche.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Étape d'exécution --------------------------------------------------------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/client/browser /usr/share/nginx/html

EXPOSE 80

# wget (busybox) est présent dans l'image de base ; endpoint servi par nginx.conf.
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://127.0.0.1/healthz || exit 1
