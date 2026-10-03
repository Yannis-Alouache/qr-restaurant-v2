-- Image de bannière (couverture) du restaurant, affichée en en-tête du menu client.
-- Même sémantique que logo_path : chemin relatif vers l'endpoint public des images.
-- IF NOT EXISTS : les bases créées avant la renumérotation V9 → V12 possèdent
-- déjà la colonne ; la migration doit rester applicable sur ces deux historiques.
ALTER TABLE restaurant ADD COLUMN IF NOT EXISTS cover_path TEXT;
