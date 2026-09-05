-- Les références d'images étaient persistées comme URLs absolues pointant vers
-- l'endpoint SeaweedFS (http://<host>/<bucket>/<key>) : illisibles depuis un autre
-- appareil que la machine hébergeant le stockage. Elles deviennent des chemins
-- relatifs servis par l'API (GET /api/images/<bucket>/<key>). Le host d'origine est
-- retiré quel qu'il soit (localhost:8333 par défaut, mais aussi tout endpoint
-- personnalisé), et les valeurs déjà relatives sont laissées intactes.
UPDATE restaurant
SET logo_path = '/api/images' || regexp_replace(logo_path, '^https?://[^/]+', '')
WHERE logo_path LIKE 'http://%';

UPDATE category
SET image_path = '/api/images' || regexp_replace(image_path, '^https?://[^/]+', '')
WHERE image_path LIKE 'http://%';

UPDATE menu_item
SET image_path = '/api/images' || regexp_replace(image_path, '^https?://[^/]+', '')
WHERE image_path LIKE 'http://%';
