package com.qrrestaurant.shared.application;

import com.qrrestaurant.shared.domain.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Nettoyage best-effort des images en stockage : supprime le fichier référencé
 * <strong>après</strong> validation de la transaction BDD (afterCommit) — ainsi un rollback
 * ne supprime jamais une image encore référencée, et une panne du stockage ne fait jamais
 * échouer l'opération métier (l'erreur est simplement logguée).
 */
@Component
public class ImageCleanup {

    private static final Logger log = LoggerFactory.getLogger(ImageCleanup.class);

    private final StorageService storage;

    public ImageCleanup(StorageService storage) {
        this.storage = storage;
    }

    public void delete(String reference) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doDelete(reference);
                }
            });
        } else {
            doDelete(reference);
        }
    }

    private void doDelete(String reference) {
        try {
            storage.delete(reference);
        } catch (RuntimeException exception) {
            log.warn("Image non supprimée du stockage (orpheline): {}", reference, exception);
        }
    }
}
