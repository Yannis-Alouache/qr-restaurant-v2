package com.qrrestaurant.auth.domain;

/**
 * Produit le jeton brut envoyé par email et son empreinte stockée en base.
 * Le brut et le hash sont dissociés : la base ne doit jamais contenir la
 * valeur cliquable du lien.
 */
public interface ResetTokenGenerator {

    String generate();

    String hash(String rawToken);
}
