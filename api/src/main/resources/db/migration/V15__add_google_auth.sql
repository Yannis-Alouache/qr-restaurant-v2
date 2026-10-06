-- Connexion Google (OAuth2 / OIDC) pour les restaurateurs.
--
-- Le mot de passe devient nullable : un compte créé via Google n'en a pas —
-- il n'est jamais utilisé comme facteur d'authentification pour ce compte.
-- `auth_provider` mémorise le mode d'authentification du compte : un compte
-- GOOGLE qui tente une connexion par mot de passe est refusé avec un message
-- explicite (et non une erreur « identifiants invalides » ambiguë).

ALTER TABLE app_user ALTER COLUMN password DROP NOT NULL;

ALTER TABLE app_user
    ADD COLUMN auth_provider TEXT NOT NULL DEFAULT 'LOCAL'
    CHECK (auth_provider IN ('LOCAL', 'GOOGLE'));
