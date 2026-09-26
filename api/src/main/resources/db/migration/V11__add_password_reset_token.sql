-- Jetons de réinitialisation de mot de passe. Le jeton reçu par l'utilisateur
-- n'est jamais stocké en clair : seule une empreinte SHA-256 est conservée,
-- avec une expiration courte et un marquage d'utilisation pour empêcher le
-- rejeu d'un lien déjà consommé.
CREATE TABLE password_reset_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_token_user_id ON password_reset_token(user_id);
