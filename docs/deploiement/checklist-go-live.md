# Checklist go-live

À dérouler dans l'ordre le jour de l'ouverture au public. Le détail de chaque
point se trouve dans [README.md](README.md).

## Infrastructure

- [ ] VPS provisionné : Ubuntu 24.04, 8 Go RAM recommandés, swap activé, accès SSH par clé
- [ ] Pare-feu : 22, 80, 443 ouverts ; 8000 et 6001 restreints à votre IP après configuration de Coolify
- [ ] Coolify installé, compte admin avec 2FA, e-mail d'instance renseigné
- [ ] DNS : `commander.` / `admin.` / `api.votredomaine.fr` → A vers l'IP du VPS, propagation vérifiée (`ping`)

## Application

- [ ] Ressource Docker Compose créée sur `main`, compose `docker/production/docker-compose.prod.yml`
- [ ] Les trois `SERVICE_FQDN_*` renseignés ; certificats Let's Encrypt émis (https en vert sur les 3 domaines)
- [ ] Toutes les variables requises remplies (modèle : `.env.production.example`) — `JWT_SECRET`, `POSTGRES_PASSWORD`, `SEAWEEDFS_*`, `MAIL_*`
- [ ] Premier déploiement : logs `api` sans erreur, Flyway a migré, `/actuator/health` → `{"status":"UP"}`
- [ ] Service `init-buckets` en `exited (0)` (normal) et buckets images créés
- [ ] Compte restaurateur réel créé ; aucune trace du compte de démo `owner@test.com` (le seed est verrouillé à `false`)
- [ ] Commande test passée depuis le parcours client, visible en temps réel dans le back-office

## Stripe

- [ ] Clés **live** dans Coolify (`STRIPE_PUBLIC_KEY`, `STRIPE_SECRET_KEY`), service `api` redémarré
- [ ] Endpoint webhook créé : `https://api.votredomaine.fr/api/webhooks/stripe`, événement `checkout.session.completed`
- [ ] `STRIPE_WEBHOOK_SECRET` (whsec_…) et `STRIPE_WEBHOOK_ENDPOINT_ID` (we_…) renseignés, `api` redémarré
- [ ] Commande réelle payée → passe de « en attente de paiement » à « payée » automatiquement, puis remboursée

## E-mail

- [ ] Relais SMTP configuré, domaine validé (SPF/DKIM) chez le fournisseur
- [ ] `MAIL_FROM` est une adresse du domaine validé
- [ ] « Mot de passe oublié ? » reçoit un e-mail, le lien fonctionne une seule fois

## Sécurité & exploitation

- [ ] `COOLIFY_DEPLOY_WEBHOOK` configuré dans GitHub (déploiement uniquement après CI verte)
- [ ] Sauvegarde PostgreSQL quotidienne planifiée (Scheduled Task Coolify) et copie **hors site** vérifiée
- [ ] Volume SeaweedFS inclus dans la sauvegarde hors site
- [ ] Restauration testée une fois sur une base jetable
- [ ] Uptime Kuma (ou équivalent) surveille les 3 domaines + `/actuator/health`
- [ ] Coolify à jour (Settings → Update)

## Légal & conformité

- [ ] Identité réelle de l'éditeur renseignée dans `admin/src/app/features/legal/legal-info.ts`
      (raison sociale, RCS/SIRET, TVA, siège, contacts, directeur de publication, hébergeur) —
      aucun champ « [À COMPLÉTER] » restant, l'avertissement a disparu de /mentions-legales
- [ ] Les trois pages publiques répondent sans authentification : `/mentions-legales`, `/terms`, `/privacy`
- [ ] Liens légaux visibles en bas des écrans connexion / inscription
- [ ] Identité de l'éditeur répercutée dans `client/src/app/features/legal/legal-info.ts`
      (miroir du fichier admin) — aucun champ « [À COMPLÉTER] » restant
- [ ] Les trois pages légales client répondent sans authentification : `/legal/cgv`,
      `/legal/confidentialite`, `/legal/mentions-legales`
- [ ] Liens légaux visibles côté client : ligne CGV/confidentialité avant paiement sur
      `/checkout/...`, pied de page sur confirmation et annulation de commande
- [ ] Police Inter servie depuis le domaine (aucune requête vers fonts.googleapis.com — vérifier
      l'onglet réseau sur /login), cohérent avec la section « Cookies et traceurs » de la /privacy
- [ ] `MAIL_FROM` cohérent avec la marque (signature « L'équipe Menzo ») et le domaine validé

## Après ouverture

- [ ] Surveiller les logs `api` les premiers jours (échecs webhook Stripe, erreurs SMTP)
- [ ] Vérifier la première sauvegarde hebdomadaire complète
