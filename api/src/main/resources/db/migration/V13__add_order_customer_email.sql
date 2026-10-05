-- Email client collecté par Stripe Checkout (champ obligatoire du formulaire
-- de paiement) et transmis par le webhook checkout.session.completed. Sert
-- uniquement à l'envoi du reçu de commande ; nullable : les commandes antérieures
-- et les webhooks sans email client restent valides.
ALTER TABLE order_table ADD COLUMN customer_email TEXT;
