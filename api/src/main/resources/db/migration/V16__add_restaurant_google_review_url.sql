-- Lien d'avis Google (« laisser un avis » de la fiche Google Business) choisi
-- par le restaurateur dans le back office ; le menu client s'en sert pour
-- inviter les clients à noter l'établissement.
-- IF NOT EXISTS : même sémantique que V12, la migration doit rester
-- applicable sur tous les historiques de base existants.
ALTER TABLE restaurant ADD COLUMN IF NOT EXISTS google_review_url TEXT;
