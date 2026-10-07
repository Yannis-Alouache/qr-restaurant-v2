# Déploiement en production avec Coolify

Ce guide mène du « rien » à l'application en ligne, vendable, avec HTTPS,
déploiements par `git push` et sauvegardes. Tout ce qui était faisable sans
serveur est déjà dans le dépôt ; ce qui reste à faire nécessite votre VPS.

## Architecture déployée

```
                        VPS (Coolify)
                ┌────────────────────────────────────────┐
Internet ──────►│ Traefik (TLS Let's Encrypt, par Coolify)│
                │   │                    │                │
                │   ▼                    ▼                │
                │ client:80            admin:80           │
                │ (nginx, Angular)   (nginx, Angular)     │
                │   │  ▲               │  ▲              │
                │   │  └──────┬────────┘  │              │
                │   ▼         ▼           ▼              │
                │  /api, /ws  ►  api:8080 (Spring Boot)  │
                │                   │          │         │
                │                   ▼          ▼         │
                │             postgres:5432  seaweedfs   │
                └────────────────────────────────────────┘
                    ▲                            ▲
            Stripe webhooks → api.votredomaine.fr │
                     relais SMTP externe (Brevo…) ─┘
```

- **Same-origin par construction** : chaque frontend nginx proxifie `/api` et
  `/ws` vers le conteneur API. Le SPA n'appelle que son propre domaine :
  pas de CORS, cookies JWT `SameSite=Lax` + `Secure` (profil `prod`).
- **Rien d'exposé inutilement** : PostgreSQL et SeaweedFS n'ont aucun port
  publié. Seuls les trois domaines publics répondent.
- **Migrations automatiques** : Flyway applique les migrations au démarrage
  de l'API ; une base vierge se prépare toute seule au premier lancement.
- **Le seed de démo est verrouillé à `false`** dans le compose de prod, même
  si la variable est ajoutée par erreur dans Coolify.

## Ce que le dépôt fournit déjà

| Élément | Emplacement |
| --- | --- |
| Image API (multi-stage Maven → JRE 21, non-root, healthcheck) | `api/Dockerfile` |
| Image back-office (build Angular → nginx + proxy `/api`, `/ws`) | `admin/Dockerfile`, `admin/nginx.conf` |
| Image parcours client (idem) | `client/Dockerfile`, `client/nginx.conf` |
| Stack de production complète compatible Coolify | `docker/production/docker-compose.prod.yml` |
| Modèle de variables de production documenté | `.env.production.example` |
| Déclenchement du déploiement après CI verte | `.github/workflows/ci.yml` (job `deploy`) |

## Ce qu'il vous reste à fournir

1. **Un VPS** (étape 1) — 2 à 4 vCPU, **8 Go de RAM** recommandés, Ubuntu 24.04.
2. **Un nom de domaine** (étape 2) — chez OVH, Gandi, Cloudflare… peu importe.
3. **Des clés Stripe live**, l'**activation de Connect** (profil Express) et
   un endpoint webhook à deux événements (étape 8).
4. **Un relais SMTP** pour les e-mails transactionnels (étape 9) — ex. Brevo,
   gratuit à 300 e-mails/jour, suffisant pour démarrer.
5. **Optionnel** : un client OAuth Google pour la connexion « Continuer avec
   Google » des restaurateurs (étape 10).
6. **Recommandé** : un compte Sentry gratuit pour l'alerte sur les erreurs
   serveur (webhook Stripe, SMTP, 500) et une sonde de disponibilité
   UptimeRobot (étape 11).

---

## Étape 1 — Le VPS

- Hébergeur au choix ; Hetzner (`CPX31`, ~15 €/mois) et OVH offrent un bon
  rapport prix/performance avec des datacenters en Europe (RGPD).
- OS : **Ubuntu 24.04**, connexion par clé SSH, authentification par mot de
  passe désactivée si l'hébergeur ne le fait pas déjà.
- Pare-feu : ouvrez **22** (SSH), **80** et **443** (HTTP/TLS public), **8000**
  (interface Coolify) et **6001** (terminal temps réel de Coolify). Une fois
  Coolify configuré, restreignez 8000 et 6001 à votre IP dans le pare-feu du
  VPS : l'interface n'a pas vocation à être publique.
- Ajoutez du swap (2–4 Go) si le VPS a 8 Go ou moins : les builds Docker
  (Maven) sont les pics mémoire les plus forts.

```bash
# sur le VPS, en root
fallocate -l 4G /swapfile && chmod 600 /swapfile \
  && mkswap /swapfile && swapon /swapfile \
  && echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

## Étape 2 — Le DNS

Chez votre registrar, créez trois enregistrements **A** pointant vers l'IP
publique du VPS (choisissez vos sous-domaines, adaptez partout ensuite) :

| Type | Nom | Valeur |
| --- | --- | --- |
| A | `commander.votredomaine.fr` | IP du VPS |
| A | `admin.votredomaine.fr` | IP du VPS |
| A | `api.votredomaine.fr` | IP du VPS |

Attendez la propagation (quelques minutes chez la plupart des registrar) :
Let's Encrypt doit pouvoir joindre le VPS sur ces noms pour émettre les
certificats.

## Étape 3 — Installer Coolify

Sur le VPS :

```bash
curl -fsSL https://cdn.coollabs.io/coolify/install.sh | bash
```

Le script installe Docker s'il manque, Coolify et son proxy Traefik, et ouvre
les ports listés plus haut. À la fin il affiche l'URL de l'interface
(`http://IP_DU_VPS:8000`) et les identifiants d'un compte temporaire.

Immédiatement après la première connexion :

1. Créez **votre** compte administrateur (le compte temporaire est affiché à
   l'écran au premier lancement, changez-le) et activez la 2FA.
2. Suivez l'assistant « localhost » : Coolify se gère lui-même sur le VPS.
3. Dans **Settings**, renseignez l'e-mail de l'instance (utilisé par Let's
   Encrypt) et vérifiez que le proxy actif est **Traefik v2+** (défaut).

## Étape 4 — Créer la ressource de déploiement

1. Créez un **Projet** (ex. « QR Restaurant ») puis, dans un environnement
   **production**, **+ New Resource**.
2. Connectez le dépôt GitHub si ce n'est pas déjà fait (application GitHub
   Coolify, accès au dépôt `qr-restaurant-v2`).
3. Choisissez le dépôt et la branche `main`, puis le build pack
   **Docker Compose**.
4. Dans la configuration de la ressource, indiquez le chemin du compose :
   `docker/production/docker-compose.prod.yml`.
5. **Ne déployez pas encore** — il manque les domaines et les variables.

Coolify lit le compose et crée un conteneur par service (`postgres`, `api`,
`client`, `admin`, `seaweedfs`, plus les deux jobs one-shot).

## Étape 5 — Domaines et variables d'environnement

### Domaines

Dans l'onglet **Environment Variables**, Coolify a détecté trois variables
spéciales de type « Domaine ». Renseignez-les avec vos URLs https :

| Variable | Valeur |
| --- | --- |
| `SERVICE_FQDN_CLIENT_80` | `https://commander.votredomaine.fr` |
| `SERVICE_FQDN_ADMIN_80` | `https://admin.votredomaine.fr` |
| `SERVICE_FQDN_API_8080` | `https://api.votredomaine.fr` |

Coolify génère alors les labels Traefik : routage par domaine + certificats
Let's Encrypt automatiques.

### Variables d'environnement

Toujours dans **Environment Variables**, complétez les variables restantes.
Le modèle `.env.production.example` à la racine documente chacune ; en résumé :

| Variable | D'où vient la valeur |
| --- | --- |
| `POSTGRES_PASSWORD` | `openssl rand -base64 48` |
| `JWT_SECRET` | `openssl rand -base64 48` — **obligatoire**, l'API refuse de démarrer sans |
| `SEAWEEDFS_ACCESS_KEY` / `SEAWEEDFS_SECRET_KEY` | `openssl rand -hex 16` (deux valeurs distinctes) |
| `CLIENT_BASE_URL` / `ADMIN_BASE_URL` | les mêmes URLs que les `SERVICE_FQDN_*` client et admin |
| `STRIPE_PUBLIC_KEY` / `STRIPE_SECRET_KEY` | clés **test** pour la mise en route, **live** avant ouverture (étape 8) |
| `STRIPE_WEBHOOK_SECRET` / `STRIPE_WEBHOOK_ENDPOINT_ID` | après création de l'endpoint webhook (étape 8) — laissées vides pour le premier déploiement |
| `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | votre relais SMTP (étape 9) — `MAIL_HOST` obligatoire, l'API refuse de démarrer sans |

Les autres variables (`POSTGRES_DB`, `POSTGRES_USER`, `MAIL_PORT`,
`MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_SMTP_EHLO_NAME`,
`PASSWORD_RESET_TOKEN_TTL_MINUTES`, `CORS_EXTRA_ORIGIN_PATTERNS`) ont des
défauts raisonnables dans le compose ; ne les définissez que si besoin.

## Étape 6 — Premier déploiement et vérifications

Lancez **Deploy**. Le premier build compile Maven et les deux frontends sur le
VPS : comptez 5 à 15 minutes. Les suivants profitent du cache Docker.

Vérifications, dans l'ordre :

1. **Logs `api`** (onglet Logs) : démarrage Spring, aucune ligne
   `APPLICATION FAILED TO START` (elle signale une variable prod manquante) ;
   Flyway applique les migrations sur la base vierge.
2. `https://api.votredomaine.fr/actuator/health` → `{"status":"UP"}`.
3. `https://commander.votredomaine.fr` → l'application client se charge ; les
   assets répondent en 200 (onglet Réseau des devtools).
4. `https://admin.votredomaine.fr` → back-office ; créez votre compte
   restaurateur et votre restaurant réel.
5. **Temps réel** : passez une commande depuis le menu client (en mode Stripe
   test) et vérifiez qu'elle apparaît dans le back-office sans rechargement.
6. Le service `init-buckets` apparaît « exited (0) » : c'est son état normal
   (il crée les buckets d'images puis s'arrête).

## Étape 7 — Déploiements au quotidien

Le flux voulu : **`git push` sur `main` → CI verte → déploiement**.

1. La CI GitHub (`.github/workflows/ci.yml`) exécute tests API, tests/builds
   frontends, e2e Playwright, puis le job `deploy`.
2. Le job `deploy` appelle l'URL de déploiement Coolify si le secret
   `COOLIFY_DEPLOY_WEBHOOK` est configuré. Pour le récupérer : dans Coolify,
   votre ressource → onglet **Webhook** → « Deploy Trigger URL » ; ajoutez-le
   dans GitHub : *Settings → Secrets and variables → Actions → New repository
   secret* sous le nom `COOLIFY_DEPLOY_WEBHOOK`.
3. Sans ce secret, le job s'achève silencieusement : la CI reste verte et
   aucun déploiement ne part — configurez-le dès que Coolify tourne.
4. Alternative simple : enregistrer l'URL Deploy Trigger comme webhook GitHub
   (*Settings → Webhooks*) déploie à chaque push, même si la CI est rouge —
   la voie du job `deploy` est préférable.

Un déploiement reconstruit les images modifiées et recrée les conteneurs
changés ; les données (volumes PostgreSQL et SeaweedFS) ne bougent pas.

## Étape 8 — Stripe en mode live

1. Dashboard Stripe → **Developers → API keys**, basculez en *Live* :
   remplacez `STRIPE_PUBLIC_KEY` et `STRIPE_SECRET_KEY` dans Coolify.
2. **Activez Connect sur le compte plateforme** — obligatoire une seule fois,
   et uniquement en live (en mode test, Connect est actif par défaut) :
   - Dashboard → **Connect → Get started**, choisissez le type de compte
     **Express** (le formulaire demande ensuite les informations légales de
     votre plateforme ; c'est le même compte que vos clés API).
   - Sans profil Connect actif, la création des comptes restaurateurs échoue
     au clic sur « Connecter mon compte Stripe » : erreur 503 côté admin,
     message Stripe détaillé dans les logs de l'API.
3. **Developers → Webhooks → Add endpoint** :
   - URL : `https://api.votredomaine.fr/api/webhooks/stripe`
   - Événements : `checkout.session.completed` (confirmation des commandes
     payées) **et** `account.updated` (statut d'onboarding Stripe Connect des
     restaurateurs — sans lui, le badge « Paiements en ligne » de leurs
     Paramètres ne se met plus à jour automatiquement)
4. Renseignez le `whsec_...` de l'endpoint dans `STRIPE_WEBHOOK_SECRET` et
   l'identifiant `we_...` dans `STRIPE_WEBHOOK_ENDPOINT_ID` (l'API vérifie au
   démarrage la cohérence de version d'API et loggue un WARN en cas d'écart).
5. **Restart** du service `api` dans Coolify pour prendre les nouvelles
   variables. Par défaut les comptes Express sont créés en France
   (`STRIPE_CONNECT_COUNTRY=FR`, cf. `.env.production.example`) — ne
   surchargez la variable que si vos restaurateurs sont ailleurs.
6. Testez une commande réelle à montant minimum, puis remboursez-la depuis
   le back-office (bouton de remboursement existant).
7. Testez le parcours restaurateur : depuis **Paramètres → Paiements en
   ligne**, cliquez « Connecter mon compte Stripe », complétez le formulaire
   Stripe (vraies informations en live) et revenez dans l'admin : le badge
   « Compte Stripe connecté » doit apparaître, puis une commande réelle doit
   être encaissée et versée sur ce compte.

> À chaque montée de version de `stripe-java` (pom.xml), mettez à jour la
> version d'API de l'endpoint webhook dans le dashboard **dans le même
> changement** — c'est ce que contrôle `STRIPE_WEBHOOK_ENDPOINT_ID`.

## Étape 9 — Le SMTP (e-mails transactionnels)

Le « mot de passe oublié » est muet sans relais joignable ; l'API **refuse de
démarrer** en prod sans `MAIL_HOST`.

- Créez un compte Brevo (ou équivalent), validez votre domaine
  (enregistrements SPF/DKIM fournis par le fournisseur — indispensable pour
  ne pas finir en spam).
- Renseignez `MAIL_HOST` (`smtp-relay.brevo.com` chez Brevo), `MAIL_PORT=587`,
  `MAIL_USERNAME`/`MAIL_PASSWORD` (clés SMTP), et `MAIL_FROM` avec une adresse
  du domaine validé (ex. `no-reply@votredomaine.fr`).
- Vérifiez avec « Mot de passe oublié ? » depuis l'admin : le mail doit
  arriver, pointer vers `https://admin.votredomaine.fr/reset-password?token=…`
  et fonctionner une seule fois.

## Étape 10 — Connexion Google (optionnelle)

Les restaurateurs peuvent se connecter au back-office avec Google
(« Continuer avec Google » sur les pages Connexion / Inscription). Sans
configuration, la fonctionnalité est simplement masquée — rien d'autre ne
change.

> **Procédure pas à pas** pour créer le projet Google Cloud, l'écran de
> consentement et récupérer le Client ID / secret :
> [connexion-google.md](../connexion-google.md).

- Dans [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
  (APIs & Services > Credentials), créez un **client OAuth 2.0** de type
  « Application Web ».
- Ajoutez l'**URI de redirection autorisée** :
  `https://admin.votredomaine.fr/api/auth/oauth2/code/google`
  (la danse OAuth traverse le proxy nginx de l'admin, qui transmet `/api`
  vers l'API — comme le reste des appels).
- Renseignez dans Coolify : `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` et
  `GOOGLE_REDIRECT_URI` (cette dernière **identique** à l'URI déclarée chez
  Google), puis redéployez.
- Vérification : le bouton apparaît sur la page de connexion de l'admin, et
  `GET https://api.votredomaine.fr/api/auth/providers` renvoie
  `{"google": true}`.

Au premier passage, le compte est créé à partir de l'email Google **vérifié**
; un restaurateur ayant déjà un compte local au même email peut dès lors se
connecter des deux façons. Un compte créé via Google n'a pas de mot de passe :
la connexion par mot de passe lui est refusée avec un message explicite.

## Étape 11 — Monitoring des erreurs (Sentry, gratuit)

Un système qui encaisse les paiements d'autres commerces ne peut pas découvrir
ses pannes par les réclamations clients. L'API signale chaque erreur serveur
(webhook Stripe rejeté, API Stripe injoignable, échec SMTP, tout 500) à
**Sentry**, qui envoie une alerte e-mail — plan Developer gratuit, sans carte
bancaire.

> **Procédure pas à pas** (compte, DSN, test de bout en bout, sonde
> UptimeRobot) : [monitoring.md](monitoring.md).

En résumé : créer le compte et le projet, renseigner `SENTRY_DSN` dans
Coolify, redémarrer `api`, puis vérifier avec un webhook Stripe à signature
bidon que l'issue apparaît et que l'e-mail part.

## Sauvegardes

**Base PostgreSQL** — créez une **Scheduled Task** Coolify sur la ressource
(fréquence recommandée : quotidienne) exécutant :

```bash
docker exec $(docker ps -qf name=postgres) pg_dump -U qr_user -d qr_restaurant \
  | gzip > /var/backups/qr-restaurant/db-$(date +%F-%H%M).sql.gz \
  && find /var/backups/qr-restaurant -name '*.sql.gz' -mtime +14 -delete
```

(Adaptez `qr_user`/`qr_restaurant` si vous avez changé `POSTGRES_USER`/`POSTGRES_DB` ;
créez le dossier au préalable : `mkdir -p /var/backups/qr-restaurant`.)

Une sauvegarde qui reste sur le VPS ne protège pas de la perte du VPS :
envoyez-la hors site. Le plus simple depuis Coolify est une deuxième tâche
`rclone` vers n'importe quel stockage S3 (Wasabi, Scaleway…) :

```bash
rclone copy /var/backups/qr-restaurant remote:qr-restaurant-backups --max-age 48h
```

**Restauration** :

```bash
gunzip -c db-2026-10-04-0330.sql.gz \
  | docker exec -i $(docker ps -qf name=postgres) psql -U qr_user -d qr_restaurant
```

**Images restaurant** — elles vivent dans le volume `seaweedfs_data` : incluez
`/var/lib/docker/volumes/*seaweedfs_data*/_data` dans vos copies hors site
(rclone ou instantané disque de l'hébergeur).

## Surveillance et maintenance

- **Erreurs applicatives** : Sentry (étape 11) alerte par e-mail sur chaque
  nouvelle erreur serveur — inutile de lire les logs « au cas où ».
- **Disponibilité** : tous les conteneurs ont des healthchecks ; Coolify les
  affiche (et redémarre un service malsain si « auto-restart » est activé sur
  la ressource). Complétez par une sonde **extérieure** (UptimeRobot, étape 11)
  : Sentry ne peut pas signaler un serveur mort, et Coolify ne le sait que
  depuis lui-même.
- Déployez **Uptime Kuma** (disponible en un clic dans Coolify) sur un autre
  serveur ou chez un hébergeur gratuit, pour être alerté si les 3 domaines
  publics ou `/actuator/health` ne répondent plus.
- Mettez Coolify à jour régulièrement (**Settings → Update**), les images de
  base via un redéploiement ponctuel (`Deploy` sans changement de code).
- Les logs API (une ligne par requête `/api`) et logs Traefik se lisent depuis
  l'interface Coolify.

## Rollback

Chaque déploiement apparaît dans l'onglet **Deployments** de la ressource.
Coolify conserve l'historique : **Redeploy** sur un commit antérieur
reconstruit l'état d'alors. Les migrations Flyway sont prévues pour ne jamais
casser la version précédente de l'API (migrations additives).

## Limites connues et suites

- **VPS unique = point de défaillance** : acceptable pour se lancer ; les
  sauvegardes hors site (ci-dessus) sont le filet. Une seconde voie serait
  une base managée (ex. Neon, Scaleway) + Coolify pour le reste.
- **Builds sur le VPS** : simples et sans registre à gérer. Si les temps de
  déploiement deviennent gênants, publier des images vers GHCR depuis GitHub
  Actions et référencer `image:` dans le compose découple build et run.
- **Chiffrement au repos** : PostgreSQL est dans un volume Docker standard ;
  activez le chiffrement du disque de l'hébergeur ou un volume chiffré si vos
  obligations l'exigent.
