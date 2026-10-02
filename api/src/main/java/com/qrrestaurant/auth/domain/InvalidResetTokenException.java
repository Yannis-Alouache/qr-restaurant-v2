package com.qrrestaurant.auth.domain;

/** Message volontairement identique pour un jeton inconnu, expiré ou déjà utilisé : pas d'indice sur l'état réel. */
public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("Ce lien de réinitialisation est invalide ou expiré");
    }
}
