# QR Restaurant v2

Monorepo d’une solution de commande au restaurant avec :

- `api/` : backend Spring Boot 3 / Java 21 / PostgreSQL / Flyway / Stripe / WebSocket STOMP
- `admin/` : back-office Angular pour le restaurateur
- `client/` : parcours client Angular pour commander et suivre sa commande
- `docker/` : dépendances locales (PostgreSQL, SeaweedFS)
- `e2e/` : preuves navigateur Playwright sur les parcours critiques

## Prérequis

| Outil | Version recommandée | Usage |
| --- | --- | --- |
| Java | 21 | API Spring Boot |
| Node.js | 22+ | Admin, client, e2e Playwright |
| npm | 10+ | Dépendances front et e2e |
| Docker | récent | PostgreSQL local, optionnellement SeaweedFS |

## Variables d’environnement

Copiez `.env.example` vers `.env` et adaptez si besoin.

| Variable | Obligatoire | Rôle | Valeur locale par défaut |
| --- | --- | --- | --- |
| `DATABASE_URL` | oui | JDBC PostgreSQL de l’API | `jdbc:postgresql://localhost:5432/qr_restaurant` |
| `POSTGRES_DB` | oui | nom de base Docker | `qr_restaurant` |
| `POSTGRES_USER` | oui | utilisateur PostgreSQL | `qr_user` |
| `POSTGRES_PASSWORD` | oui | mot de passe PostgreSQL | `qr_password` |
| `PGADMIN_DEFAULT_EMAIL` | non | email de connexion pgAdmin local | `admin@example.com` |
| `PGADMIN_DEFAULT_PASSWORD` | non | mot de passe de connexion pgAdmin local | `admin` |
| `STRIPE_SECRET_KEY` | oui | clé Stripe backend | `sk_test_xxx` |
| `STRIPE_PUBLIC_KEY` | oui | clé publique Stripe | `pk_test_xxx` |
| `STRIPE_WEBHOOK_SECRET` | non mais requis pour les webhooks Stripe | validation des signatures webhook | `whsec_xxx` |
| `SEAWEEDFS_S3_ENDPOINT` | selon usage images | endpoint S3 SeaweedFS | `http://localhost:8333` |
| `SEAWEEDFS_ACCESS_KEY` | selon usage images | clé S3 SeaweedFS | `any` |
| `SEAWEEDFS_SECRET_KEY` | selon usage images | secret S3 SeaweedFS | `any` |
| `JWT_SECRET` | oui | signature JWT | voir `.env.example` |
| `JWT_COOKIE_SECURE` | non | cookie `Secure` (HTTPS uniquement) — `true` par défaut en profil `prod`, refus de démarrer sans `JWT_SECRET` fort | `false` en local |
| `SEED_DEMO_DATA` | non | active le jeu de données de démo (V2) à la migration | `true` en local, voir ci-dessous |
| `CLIENT_BASE_URL` | oui | URL publique client | `http://localhost:4300` |
| `ADMIN_BASE_URL` | oui | URL publique admin | `http://localhost:4200` |

## Démarrage local

1. Installer les dépendances :
   - `cd admin && npm ci`
   - `cd client && npm ci`
   - `npm ci`
2. Démarrer PostgreSQL et pgAdmin :
   - `docker compose -f docker/docker-compose.yml up -d postgres pgadmin`
3. Démarrer l’API :
   - `cd api && mvn spring-boot:run`
4. Démarrer les frontends :
   - `cd admin && npm start`
   - `cd client && npm start`

En local, les proxies Angular redirigent `/api` et `/ws` vers `http://localhost:8080`.

### Explorer la base avec pgAdmin

- URL : `http://localhost:5050`
- login pgAdmin : `PGADMIN_DEFAULT_EMAIL` / `PGADMIN_DEFAULT_PASSWORD`
- serveur préconfiguré : `QR Restaurant Local`
- mot de passe PostgreSQL à saisir lors de la première connexion : `POSTGRES_PASSWORD` (par défaut `qr_password`)
- si vous changez `POSTGRES_DB` ou `POSTGRES_USER`, adaptez la connexion pgAdmin une fois connecté

### Démo locale seedée

Le seed de démonstration (migration V2) est **désactivé par défaut** : une base vierge migrée en production ne contient aucun compte de démo. Il ne s'applique que si `SEED_DEMO_DATA=true` (placeholder Flyway `seed_demo_data`) — c'est le cas du `.env` local et de la CI e2e.

Avec le seed actif, le compte de démo est :

- email : `owner@test.com`
- mot de passe : `Secret123!`

## Commandes principales

| Surface | Installer | Tester | Builder | Lancer |
| --- | --- | --- | --- | --- |
| API | Maven | `cd api && mvn -q test` | `cd api && mvn -q test` | `cd api && mvn spring-boot:run` |
| Admin | `cd admin && npm ci` | `cd admin && npm test -- --watch=false` | `cd admin && npm run build` | `cd admin && npm start` |
| Client | `cd client && npm ci` | `cd client && npm test -- --watch=false` | `cd client && npm run build` | `cd client && npm start` |
| E2E navigateur | `npm ci` | `npm run test:e2e` | n/a | démarre les serveurs via Playwright |

### Reset local de la base

- `npm run reset-db`
- la commande n'est autorisee que si `APP_ENV=local` dans votre `.env`
- la commande refuse de s’exécuter en CI
- au lancement, une confirmation interactive `oui / non` est demandée avant de vider la base

## Ce qui est prouvé automatiquement

### Flyway

- base vide : migrations complètes appliquées normalement ;
- base existante déjà initialisée sans `flyway_schema_history` : baseline contrôlé puis migrations de rattrapage ;
- base partiellement initialisée : démarrage refusé explicitement au lieu d’un baseline silencieux.

### WebSocket

- les origines autorisées HTTP et WebSocket sont résolues à partir de `CLIENT_BASE_URL`, `ADMIN_BASE_URL` et des URLs locales ;
- les parcours navigateur Playwright vérifient les mises à jour temps réel côté client et côté admin.

### Stripe : réel vs mocké

| Couche | Ce qui est validé réellement | Ce qui reste mocké |
| --- | --- | --- |
| `StripePaymentGateway` | construction des `SessionCreateParams` Stripe SDK (montant, metadata, transfer, success/cancel URLs) | l’appel réseau vers l’API Stripe |
| `StripeWebhookController` | vérification réelle de signature webhook et désérialisation d’event Stripe signé | aucun mock Stripe dans ce contrôle |
| API acceptance / e2e fonctionnels | cycle commande → paiement confirmé → statut temps réel | la session Checkout hébergée Stripe elle-même |

Pour une validation manuelle bout en bout avec Stripe réel en local, utilisez de vraies clés de test Stripe et un secret webhook valable, puis rejouez les événements via Stripe CLI.

## Webhooks Stripe en local

Stripe ne peut pas appeler `localhost` : il faut le CLI pour forwarder les événements (sinon les commandes payées restent `en_attente_paiement` et n'apparaissent jamais côté admin).

```bash
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```

Le secret affiché (`whsec_...`) doit correspondre à `STRIPE_WEBHOOK_SECRET` du `.env` (le CLI réutilise le même secret sur une même machine). Pour rejouer un événement déjà émis : `stripe events resend <evt_id>`.

L'API accepte les événements quelle que soit leur version d'API : le SDK Java épine la sienne (`Stripe.API_VERSION`) et un fallback désérialise quand même les événements rendus dans une autre version (compte, CLI).

### En production

- créez le webhook endpoint dans le dashboard Stripe avec la **même version d'API que le SDK** (visible au démarrage de l'API dans les logs) ;
- renseignez `STRIPE_WEBHOOK_ENDPOINT_ID` (identifiant `we_...`) : au démarrage, l'API compare la version de l'endpoint avec celle du SDK et loggue un **WARN** en cas d'écart ;
- à chaque montée de version de `stripe-java`, mettez à jour la version de l'endpoint dans le dashboard **dans le même changement**.

## E2E navigateur

Les scénarios Playwright couvrent :

1. le back-office admin qui reçoit une commande payée en temps réel et peut l’avancer ;
2. la page client de confirmation qui se met à jour sans reload après webhook Stripe puis après changement d’état en cuisine.

Pré-requis :

- PostgreSQL local démarré ;
- dépendances `admin`, `client` et racine installées ;
- navigateur Chromium Playwright installé (`npx playwright install chromium`).

## CI

Une base de CI est fournie dans `.github/workflows/ci.yml` pour :

- les tests API ;
- les tests et builds admin/client ;
- les e2e navigateur Playwright avec PostgreSQL Docker.
