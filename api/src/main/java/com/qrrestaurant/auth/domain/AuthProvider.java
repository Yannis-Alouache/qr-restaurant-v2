package com.qrrestaurant.auth.domain;

/**
 * Mode d'authentification d'un compte restaurateur.
 *
 * <ul>
 *   <li>{@link #LOCAL} : email + mot de passe (inscription classique) ;</li>
 *   <li>{@link #GOOGLE} : compte créé via Google (pas de mot de passe local).</li>
 * </ul>
 *
 * Un compte LOCAL peut aussi se connecter avec Google (l'email étant vérifié
 * par Google, la correspondance suffit) ; la réciproque est fausse : un compte
 * GOOGLE n'a pas de mot de passe et la connexion par mot de passe est refusée.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
