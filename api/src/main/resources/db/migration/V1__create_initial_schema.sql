CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Restaurant owners (authentication)
CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Restaurants
CREATE TABLE restaurant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    address TEXT,
    logo_path TEXT,
    theme_id TEXT NOT NULL DEFAULT 'classique',
    payment_provider_account_id TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tables (restaurant physical tables)
CREATE TABLE restaurant_table (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    number INT NOT NULL,
    UNIQUE(restaurant_id, number)
);

-- Menu categories
CREATE TABLE category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    image_path TEXT,
    position INT NOT NULL DEFAULT 0,
    has_menu BOOLEAN NOT NULL DEFAULT false
);

-- Menu items
CREATE TABLE menu_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    image_path TEXT,
    available BOOLEAN NOT NULL DEFAULT true,
    menu_variant_of UUID REFERENCES menu_item(id) ON DELETE CASCADE
);

-- Combo meal composition (sides & drinks available for menus)
CREATE TABLE menu_composition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    composition_type TEXT NOT NULL CHECK (composition_type IN ('accompagnement', 'boisson')),
    menu_item_id UUID NOT NULL REFERENCES menu_item(id) ON DELETE CASCADE,
    supplement_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    UNIQUE(restaurant_id, composition_type, menu_item_id)
);

-- Orders
CREATE TABLE order_table (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    table_id UUID NOT NULL REFERENCES restaurant_table(id) ON DELETE RESTRICT,
    status TEXT NOT NULL DEFAULT 'en_attente_paiement'
        CHECK (status IN ('en_attente_paiement', 'paiement_echoue', 'nouvelle', 'en_preparation', 'prete', 'servie')),
    total DECIMAL(10,2) NOT NULL,
    payment_transaction_id TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Order items
CREATE TABLE order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES order_table(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_item(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL,
    menu_group_id UUID,
    menu_role TEXT CHECK (menu_role IS NULL OR menu_role IN ('plat', 'accompagnement', 'boisson'))
);

-- Indexes for common queries
CREATE INDEX idx_restaurant_slug ON restaurant(slug);
CREATE INDEX idx_restaurant_user_id ON restaurant(user_id);
CREATE INDEX idx_category_restaurant_id ON category(restaurant_id);
CREATE INDEX idx_menu_item_category_id ON menu_item(category_id);
CREATE INDEX idx_menu_item_variant_of ON menu_item(menu_variant_of);
CREATE INDEX idx_menu_composition_restaurant_id ON menu_composition(restaurant_id);
CREATE INDEX idx_order_restaurant_id ON order_table(restaurant_id);
CREATE INDEX idx_order_table_id ON order_table(table_id);
CREATE INDEX idx_order_status ON order_table(status);
CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_item_menu_group_id ON order_item(menu_group_id);
