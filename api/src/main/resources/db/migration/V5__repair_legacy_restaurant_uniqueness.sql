WITH duplicated_restaurants AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at, id) AS duplicate_rank
    FROM restaurant
)
DELETE FROM restaurant
WHERE id IN (
    SELECT id
    FROM duplicated_restaurants
    WHERE duplicate_rank > 1
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = current_schema()
          AND table_name = 'restaurant'
          AND constraint_name = 'uk_restaurant_user_id'
    ) THEN
        ALTER TABLE restaurant
            ADD CONSTRAINT uk_restaurant_user_id UNIQUE (user_id);
    END IF;
END $$;
