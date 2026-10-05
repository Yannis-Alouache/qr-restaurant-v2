-- Statut d'onboarding Stripe Connect du compte Express du restaurateur :
--   NULL        aucun compte connecté (paiements en ligne indisponibles)
--   pending     compte Stripe créé, inscription (KYC) incomplète
--   active      inscription terminée : le restaurant peut encaisser
--   restricted  inscription terminée mais encaissement/versements bloqués
-- Le champ payment_provider_account_id, lui, est rempli automatiquement par le
-- flux d'onboarding (jamais saisi à la main dans l'admin).
-- Backfill : les comptes renseignés avant cette migration (seed de démo) sont
-- considérés comme actifs ; les vrais comptes passeront par le flux dédié.
ALTER TABLE restaurant ADD COLUMN IF NOT EXISTS stripe_connect_status TEXT;

UPDATE restaurant
SET stripe_connect_status = 'active'
WHERE payment_provider_account_id IS NOT NULL
  AND payment_provider_account_id <> ''
  AND stripe_connect_status IS NULL;
