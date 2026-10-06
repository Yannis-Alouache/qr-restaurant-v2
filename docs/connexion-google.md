# Connexion Google — récupérer un Client ID et un Client Secret

Procédure pas à pas pour créer le client OAuth 2.0 attendu par l'API dans
`GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` / `GOOGLE_REDIRECT_URI`
(bouton « Continuer avec Google » du back-office restaurateur).

Aucune carte bancaire ni API payante : tout se fait dans la console Google
Cloud, gratuitement.

## 1. Créer (ou choisir) le projet Google Cloud

1. Ouvrez [console.cloud.google.com](https://console.cloud.google.com) et
   connectez-vous avec n'importe quel compte Google.
2. Dans le sélecteur de projet (en haut à gauche) : **Nouveau projet** —
   nom libre, par ex. `QR Restaurant`. Le nom n'apparaît que dans votre
   console.
3. Assurez-vous que ce projet est bien sélectionné pour la suite.

## 2. Configurer l'écran de consentement OAuth

L'application ne demande que des scopes basiques (`openid`, `email`,
`profile`) : aucune validation Google n'est requise, mais l'écran doit
exister pour pouvoir créer un client.

1. Menu ☰ → **APIs & Services** → **OAuth consent screen** (dans la version
   récente de la console, la rubrique s'appelle **Google Auth Platform**).
2. Type d'utilisateur : **Externe** → **Créer**.
3. Renseignez le minimum : nom de l'application (celui que verront les
   restaurateurs, ex. `Menzo`), e-mail d'assistance et e-mail développeur.
   Logo et domaines sont optionnels à ce stade.
4. **Statut de publication** :
   - **En test** : seuls les comptes ajoutés comme « utilisateurs test »
     peuvent se connecter — pratique pour vos premiers essais (ajoutez
     votre propre compte Google dans la section Audience / Utilisateurs
     test).
   - **En production** (bouton **Publier l'application**) : tout compte
     Google peut se connecter. Avec les scopes basiques, aucun écran
     d'avertissement ni revue Google n'est nécessaire.

Vous pourrez publier plus tard : commencez en test si vous voulez.

## 3. Créer le client OAuth et récupérer les identifiants

1. Menu ☰ → **APIs & Services** → **Identifiants** (Credentials) →
   **+ Créer des identifiants** → **ID client OAuth**.
   (Nouvelle console : Google Auth Platform → **Clients** → **Créer un
   client**.)
2. Type d'application : **Application Web**.
3. **URI de redirection autorisée** — ajoutez les URIs que vous utiliserez ;
   elles doivent correspondre **au caractère près** à `GOOGLE_REDIRECT_URI` :

   | Environnement | URI de redirection |
   | --- | --- |
   | Local | `http://localhost:4200/api/auth/oauth2/code/google` |
   | Production | `https://admin.votredomaine.fr/api/auth/oauth2/code/google` |

   (Adaptez le domaine de production à vos DNS ; le chemin
   `/api/auth/oauth2/code/google` est fixe.)
4. **Créer** : une fenêtre affiche le **Client ID** (forme
   `1234…apps.googleusercontent.com`) et le **Client secret** (forme
   `GOCSPX-…`). Vous pourrez les revoir à tout moment en cliquant sur le
   client dans la liste des identifiants.

C'est tout : pas de clé API, pas d'activation d'API supplémentaire.

## 4. Renseigner l'application

| Variable | Valeur | Où |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | le Client ID de l'étape 3 | `.env` en local, variables Coolify en prod |
| `GOOGLE_CLIENT_SECRET` | le Client secret | idem |
| `GOOGLE_REDIRECT_URI` | **une seule** des URIs du tableau, celle de l'environnement courant | idem (défaut local : `http://localhost:4200/api/auth/oauth2/code/google`) |

Redémarrez l'API : le bouton apparaît sur les pages Connexion et Inscription
de l'admin, et `GET /api/auth/providers` renvoie `{"google": true}`.

## Dépannage

| Symptôme | Cause probable | Correctif |
| --- | --- | --- |
| Erreur Google `redirect_uri_mismatch` | l'URI de callback diffère de celle déclarée | recopier l'URI exacte chez Google (protocole, port, chemin) ou corriger `GOOGLE_REDIRECT_URI` |
| Erreur Google `invalid_client` | Client ID ou secret mal copié | revérifier les deux valeurs |
| « Accès bloqué », connexion limitée aux testeurs | écran de consentement resté **En test** | publier l'application, ou ajouter le compte en utilisateur test |
| Bouton « Continuer avec Google » absent | `GOOGLE_CLIENT_ID` vide ou API non redémarrée | vérifier la variable, redémarrer l'API, `/api/auth/providers` doit renvoyer `true` |
| Page de connexion revient avec `?erreur=google` sans message Google | state invalide (cookie perdu en route) ou email non vérifié chez Google | refaire l'essai en navigation normale (pas en navigation privée avec cookies bloqués) |
