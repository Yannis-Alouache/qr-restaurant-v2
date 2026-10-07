# Monitoring des erreurs — Sentry (gratuit)

L'API traite les paiements d'autres commerces : une panne du webhook Stripe ou
du SMTP qui n'est découverte que par un restaurateur furieux est inacceptable.
Depuis l'intégration Sentry, **chaque erreur serveur est signalée en ligne avec
sa stack trace et déclenche une alerte e-mail** — sans supervision manuelle des
logs.

Le plan **Developer** de Sentry est gratuit, sans carte bancaire :
5 000 événements d'erreur/mois, 1 membre, alertes e-mail incluses, rétention
30 jours. Largement suffisant pour démarrer ; en cas de dépassement, les
événements sont simplement abandonnés (aucune facturation surprise).

## Ce qui est remonté automatiquement

| Incident | Détecté où | Réponse HTTP |
| --- | --- | --- |
| Signature ou payload Stripe invalide (ex. `STRIPE_WEBHOOK_SECRET` roté côté dashboard sans mise à jour ici) | `GlobalExceptionHandler` | 400 à Stripe → il retente 3 jours puis abandonne : sans alerte, la commande payée disparaissait |
| API Stripe injoignable (création checkout, remboursement, Connect) | `GlobalExceptionHandler` | 503 |
| Échec d'envoi SMTP (mot de passe oublié, reçus de commande) | les deux mailers | aucune (échec silencieux, maintenant signalé) |
| Stockage images indisponible (SeaweedFS) | `GlobalExceptionHandler` | 503 |
| Tout autre 500 (bug, base de données…) | `GlobalExceptionHandler` | 500 — auparavant sans même une ligne de log |

Chaque type d'erreur devient une « issue » groupée dans Sentry : 100 occurrences
d'une même panne = une alerte, pas cent.

## Étape 1 — Créer le compte et le projet (2 minutes)

1. [sentry.io/signup](https://sentry.io/signup/) — choisissez **Developer
   (free)**. Pas de carte bancaire.
2. Créez un projet : plateforme **Java / Spring Boot**, nom ex. `qr-restaurant-api`.
3. Copiez le **DSN** affiché (forme `https://xxx@oXXX.ingest.sentry.io/XXX`).

## Étape 2 — Brancher l'API

Dans Coolify : votre ressource → onglet **Environment Variables** → ajoutez :

| Variable | Valeur |
| --- | --- |
| `SENTRY_DSN` | le DSN de l'étape 1 |

(`SENTRY_ENVIRONMENT=production` est déjà passé par
`docker/production/docker-compose.prod.yml` ; sans DSN, le SDK reste inactif —
en dev local, rien ne part nulle part.)

Redéployez ou **Restart** le service `api`.

## Étape 3 — Vérifier l'alerte e-mail

Dans Sentry, le projet a une règle d'alerte par défaut (**Alerts → votre
projet**) : « Send a notification for a new issue », par e-mail aux membres du
projet. Vérifiez qu'elle est **activée** et que votre adresse est bien membre
du projet. C'est cette règle qui vous réveille la nuit ; ne la désactivez pas.

## Étape 4 — Test de bout en bout

Envoyez volontairement un webhook Stripe à la signature bidon :

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://api.votredomaine.fr/api/webhooks/stripe \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=1700000000,v1=signature-bidon" \
  --data '{}'
# → 400
```

Sous ~30 secondes, une issue « Signature Stripe invalide » apparaît dans
Sentry, et l'e-mail d'alerte arrive. Si vous voulez tester la voie SMTP :
arrêtez le relais (ou mettez un `MAIL_PASSWORD` faux) puis demandez une
réinitialisation de mot de passe ; l'erreur SMTP remonte de la même façon.

## En complément : la supervision externe

Sentry ne peut rien signaler si le processus API, le VPS ou le DNS est mort —
un serveur éteint ne se dénonce pas lui-même. Il faut donc **aussi** une sonde
extérieure qui interroge l'API et alerte sur absence de réponse.

Deux options gratuites :

- **UptimeRobot** ([uptimerobot.com](https://uptimerobot.com), 50 sondes,
  vérification toutes les 5 min, alerte e-mail, zéro serveur à gérer) — le plus
  simple : créez un compte, puis un monitor **HTTP(s)** par URL :
  - `https://api.votredomaine.fr/actuator/health`
  - `https://commander.votredomaine.fr`
  - `https://admin.votredomaine.fr`

  Et dans **Alert Contacts**, vérifiez votre e-mail.
- **Uptime Kuma** (auto-hébergé, un clic dans Coolify) — plus complet, mais à
  installer **sur un autre serveur** : surveiller le VPS depuis le VPS ne voit
  pas tomber le VPS.

## Limites connues

- **Les frontends Angular ne sont pas instrumentés** : une erreur JavaScript
  côté client (client ou admin) n'apparaît pas dans Sentry. Si besoin plus tard,
  intégrer `@sentry/angular` dans `client/` et `admin/` avec le même compte
  (le plan Developer couvre plusieurs projets).
- **5 000 événements/mois** : une erreur qui se répète en boucle (ex. un cron
  externe qui martèle un endpoint cassé) peut saturer le quota. Sentry groupe
  par issue ; si un quota s'épuise, cherchez l'issue la plus fréquente plutôt
  qu'un défaut d'intégration.
- Sentry est un service tiers externe (UE) : seules des traces d'erreurs
  techniques y partent — `send-default-pii: false` est posé dans
  `application.yml`, aucune donnée personnelle ni payload de requête n'est
  envoyé.
