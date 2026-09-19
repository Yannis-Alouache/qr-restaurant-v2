-- Soft delete des articles : un article référencé par des commandes passées ne peut
-- pas être supprimé physiquement (order_item.menu_item_id ... ON DELETE RESTRICT).
-- La suppression devient un simple deleted_at ; tous les chemins de lecture filtrent
-- les lignes marquées, et l'historique des commandes reste intact.
ALTER TABLE menu_item ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
