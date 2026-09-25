-- Suivi des fichiers uploadés : permet un quota de stockage par compte.
-- Les lignes sont créées à l'upload (ImageController) et retirées quand
-- l'image est nettoyée du stockage (ImageCleanup).

CREATE TABLE uploaded_file (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    bucket TEXT NOT NULL,
    key TEXT NOT NULL UNIQUE,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_uploaded_file_owner_id ON uploaded_file(owner_id);
