-- Seed test data for development

-- Test user (password: "secret123" hashed with BCrypt)
INSERT INTO app_user (id, email, password) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'owner@test.com', '$2a$10$EqKcp1WFKWgbbKJHqTDMtOEeMWMwJYF3FcxHCijPEhMvJbGCFcSxa');

-- Test restaurant
INSERT INTO restaurant (id, user_id, name, slug, address, theme_id, payment_provider_account_id) VALUES
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Naia Burger', 'naia-burger', '12 Rue de la Paix, Paris', 'chaud', 'acct_seed_test');

-- Test tables
INSERT INTO restaurant_table (id, restaurant_id, number) VALUES
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 1),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 2),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 3);

-- Test categories
INSERT INTO category (id, restaurant_id, name, position, has_menu) VALUES
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Burgers', 0, true),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Desserts', 1, false);

-- Test menu items (Burgers - standalone + menu variants)
INSERT INTO menu_item (id, category_id, name, description, price, available) VALUES
    -- Base items
    ('e0eebc99-0001-4ef8-bb6d-6bb9bd380a01', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Burger classique', 'Steak haché, cheddar, salade, tomate, sauce maison', 6.90, true),
    ('e0eebc99-0001-4ef8-bb6d-6bb9bd380a02', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Burger bacon', 'Steak haché, cheddar, bacon croustillant, oignons caramélisés', 8.50, true),
    ('e0eebc99-0001-4ef8-bb6d-6bb9bd380a03', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'Brownie maison', 'Brownie au chocolat noir, noix de pécan', 4.50, true),
    -- Menu variants (combo meal pricing)
    ('e0eebc99-0002-4ef8-bb6d-6bb9bd380a01', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Menu Burger classique', 'Formule complète', 10.40, true),
    ('e0eebc99-0002-4ef8-bb6d-6bb9bd380a02', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Menu Burger bacon', 'Formule complète', 12.00, true);

-- Link variants to base items
UPDATE menu_item SET menu_variant_of = 'e0eebc99-0001-4ef8-bb6d-6bb9bd380a01' WHERE id = 'e0eebc99-0002-4ef8-bb6d-6bb9bd380a01';
UPDATE menu_item SET menu_variant_of = 'e0eebc99-0001-4ef8-bb6d-6bb9bd380a02' WHERE id = 'e0eebc99-0002-4ef8-bb6d-6bb9bd380a02';

-- Test menu compositions (sides and drinks for combo meals)
-- First, create the side/drink items in a dedicated way
INSERT INTO menu_item (id, category_id, name, description, price, available) VALUES
    ('f0eebc99-0001-4ef8-bb6d-6bb9bd380a01', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Frites', 'Frites croustillantes', 3.50, true),
    ('f0eebc99-0001-4ef8-bb6d-6bb9bd380a02', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Nuggets x6', 'Nuggets de poulet croustillants', 5.00, true),
    ('f0eebc99-0001-4ef8-bb6d-6bb9bd380a03', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Coca-Cola', 'Coca-Cola 33cl', 2.50, true),
    ('f0eebc99-0001-4ef8-bb6d-6bb9bd380a04', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Fanta', 'Fanta orange 33cl', 2.50, true);

-- Compositions: link sides/drinks to combo meals with supplement prices
INSERT INTO menu_composition (id, restaurant_id, composition_type, menu_item_id, supplement_price) VALUES
    ('a0eebc99-1001-4ef8-bb6d-6bb9bd380b01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'accompagnement', 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a01', 0.00),
    ('a0eebc99-1002-4ef8-bb6d-6bb9bd380b02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'accompagnement', 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a02', 1.50),
    ('a0eebc99-1003-4ef8-bb6d-6bb9bd380b03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'boisson', 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a03', 0.00),
    ('a0eebc99-1004-4ef8-bb6d-6bb9bd380b04', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'boisson', 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a04', 0.00);
