-- Autorise le nouveau statut "rembourse" (remboursement Stripe depuis l'admin).

ALTER TABLE order_table DROP CONSTRAINT IF EXISTS order_table_status_check;
ALTER TABLE order_table ADD CONSTRAINT order_table_status_check
    CHECK (status IN ('en_attente_paiement', 'paiement_echoue', 'nouvelle', 'en_preparation', 'prete', 'servie', 'rembourse'));
