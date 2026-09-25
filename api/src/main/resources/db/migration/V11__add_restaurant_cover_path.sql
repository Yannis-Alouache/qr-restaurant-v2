-- Image de bannière (couverture) du restaurant, affichée en en-tête du menu client.
-- Même sémantique que logo_path : chemin relatif vers l'endpoint public des images.
ALTER TABLE restaurant ADD COLUMN cover_path TEXT;
