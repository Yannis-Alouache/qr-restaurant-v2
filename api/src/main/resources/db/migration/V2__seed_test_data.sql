-- Données de démonstration (restaurant "Naia Burger" + compte owner@test.com).
--
-- Gate explicite : tout l'INSERT est sous la condition du placeholder Flyway
-- seed_demo_data (propriété spring.flyway.placeholders.seed_demo_data, défaut
-- false — variable SEED_DEMO_DATA). Aucun environnement n'insère ces données
-- sans les avoir demandées : en production, une base vide ne reçoit jamais de
-- compte owner@test.com. Les bases de dev l'activent via .env.
--
-- Les casts ::uuid sont requis : depuis un sous-requête VALUES, les littéraux
-- chaînes arrivent typés text et ne sont pas coercibles implicitement.
--
-- V4 et V6 (historiques des mots de passe seed) ne ciblent que ce compte :
-- elles deviennent des no-ops naturels quand ce seed est désactivé, et leurs
-- checksums sont inchangés.

INSERT INTO app_user (id, email, password)
SELECT id::uuid, email, password
FROM (
    VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'owner@test.com',
            '$2a$10$EqKcp1WFKWgbbKJHqTDMtOEeMWMwJYF3FcxHCijPEhMvJbGCFcSxa')
) AS seed(id, email, password)
WHERE '${seed_demo_data}' = 'true';

INSERT INTO restaurant (id, user_id, name, slug, address, theme_id, payment_provider_account_id)
SELECT id::uuid, user_id::uuid, name, slug, address, theme_id, payment_provider_account_id
FROM (
    VALUES ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
            'Naia Burger', 'naia-burger', '12 Rue de la Paix, Paris', 'chaud', 'acct_seed_test')
) AS seed(id, user_id, name, slug, address, theme_id, payment_provider_account_id)
WHERE '${seed_demo_data}' = 'true';

INSERT INTO restaurant_table (id, restaurant_id, number)
SELECT id::uuid, restaurant_id::uuid, number
FROM (
    VALUES ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 1),
           ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 2),
           ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 3)
) AS seed(id, restaurant_id, number)
WHERE '${seed_demo_data}' = 'true';

INSERT INTO category (id, restaurant_id, name, position, has_menu)
SELECT id::uuid, restaurant_id::uuid, name, "position", has_menu
FROM (
    VALUES ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Burgers', 0, true),
           ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Desserts', 1, false)
) AS seed(id, restaurant_id, name, position, has_menu)
WHERE '${seed_demo_data}' = 'true';

INSERT INTO menu_item (id, category_id, name, description, price, available)
SELECT id::uuid, category_id::uuid, name, description, price, available
FROM (
    VALUES ('e0eebc99-0001-4ef8-bb6d-6bb9bd380a01', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
            'Burger classique', 'Steak haché, cheddar, salade, tomate, sauce maison', 6.90, true),
           ('e0eebc99-0001-4ef8-bb6d-6bb9bd380a02', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
            'Burger bacon', 'Steak haché, cheddar, bacon croustillant, oignons caramélisés', 8.50, true),
           ('e0eebc99-0001-4ef8-bb6d-6bb9bd380a03', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
            'Brownie maison', 'Brownie au chocolat noir, noix de pécan', 4.50, true),
           ('e0eebc99-0002-4ef8-bb6d-6bb9bd380a01', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
            'Menu Burger classique', 'Formule complète', 10.40, true),
           ('e0eebc99-0002-4ef8-bb6d-6bb9bd380a02', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
            'Menu Burger bacon', 'Formule complète', 12.00, true)
) AS seed(id, category_id, name, description, price, available)
WHERE '${seed_demo_data}' = 'true';

-- Lien variantes -> article de base (menus du seed uniquement)
UPDATE menu_item
SET menu_variant_of = 'e0eebc99-0001-4ef8-bb6d-6bb9bd380a01'
WHERE id = 'e0eebc99-0002-4ef8-bb6d-6bb9bd380a01'
  AND '${seed_demo_data}' = 'true';

UPDATE menu_item
SET menu_variant_of = 'e0eebc99-0001-4ef8-bb6d-6bb9bd380a02'
WHERE id = 'e0eebc99-0002-4ef8-bb6d-6bb9bd380a02'
  AND '${seed_demo_data}' = 'true';

-- Accompagnements et boissons disponibles dans les menus
INSERT INTO menu_item (id, category_id, name, description, price, available)
SELECT id::uuid, category_id::uuid, name, description, price, available
FROM (
    VALUES ('f0eebc99-0001-4ef8-bb6d-6bb9bd380a01', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
            'Frites', 'Frites croustillantes', 3.50, true),
           ('f0eebc99-0001-4ef8-bb6d-6bb9bd380a02', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
            'Nuggets x6', 'Nuggets de poulet croustillants', 5.00, true),
           ('f0eebc99-0001-4ef8-bb6d-6bb9bd380a03', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
            'Coca-Cola', 'Coca-Cola 33cl', 2.50, true),
           ('f0eebc99-0001-4ef8-bb6d-6bb9bd380a04', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
            'Fanta', 'Fanta orange 33cl', 2.50, true)
) AS seed(id, category_id, name, description, price, available)
WHERE '${seed_demo_data}' = 'true';

-- Compositions : accompagnements/boissons liés aux menus, avec suppléments
INSERT INTO menu_composition (id, restaurant_id, composition_type, menu_item_id, supplement_price)
SELECT id::uuid, restaurant_id::uuid, composition_type, menu_item_id::uuid, supplement_price
FROM (
    VALUES ('a0eebc99-1001-4ef8-bb6d-6bb9bd380b01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
            'accompagnement', 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a01', 0.00),
           ('a0eebc99-1002-4ef8-bb6d-6bb9bd380b02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
            'accompagnement', 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a02', 1.50),
           ('a0eebc99-1003-4ef8-bb6d-6bb9bd380b03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
            'boisson', 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a03', 0.00),
           ('a0eebc99-1004-4ef8-bb6d-6bb9bd380b04', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
            'boisson', 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a04', 0.00)
) AS seed(id, restaurant_id, composition_type, menu_item_id, supplement_price)
WHERE '${seed_demo_data}' = 'true';
