# QR Restaurant Ordering — Application Specification

## 1. Overview

A QR-code-based restaurant ordering system where customers scan a QR code at their table to browse the menu, compose orders (standalone items or combo meals), and pay online. Restaurant owners manage their menu, track orders in real-time, and configure their restaurant branding through a separate admin interface.

**User roles:**
- **Customer** (anonymous): Scans QR code, browses menu, places orders, pays online
- **Restaurant Owner** (authenticated): Manages menu, orders, settings, and generates QR codes

---

## 2. Data Model

### 2.1 Restaurant

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| id | uuid | auto-generated | random uuid | Primary key |
| user_id | uuid | yes | — | References authenticated user. Cascade delete |
| name | text | yes | — | — |
| slug | text | yes | — | Unique. URL-safe identifier |
| address | text | no | null | — |
| logo_path | text | no | null | Path in logo storage bucket |
| theme_id | text | no | "classique" | One of: "classique", "chaud", "nature", "elegant" |
| payment_provider_account_id | text | no | null | Connected payment provider account |
| created_at | timestamp | auto-generated | current time | — |

**Relationships:**
- One restaurant has many tables
- One restaurant has many categories
- One restaurant has many orders
- One restaurant has many menu compositions

### 2.2 Table

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| id | uuid | auto-generated | random uuid | Primary key |
| restaurant_id | uuid | yes | — | References restaurant. Cascade delete |
| number | integer | yes | — | Unique per restaurant |

**Relationships:**
- Many tables belong to one restaurant
- One table has many orders

### 2.3 Category

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| id | uuid | auto-generated | random uuid | Primary key |
| restaurant_id | uuid | yes | — | References restaurant. Cascade delete |
| name | text | yes | — | — |
| image_path | text | no | null | Path in category-images storage bucket |
| position | integer | yes | 0 | Ordering index for display |
| has_menu | boolean | yes | false | Whether items in this category support combo meal mode |

**Relationships:**
- Many categories belong to one restaurant
- One category has many menu items

### 2.4 Menu Item

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| id | uuid | auto-generated | random uuid | Primary key |
| category_id | uuid | yes | — | References category. Cascade delete |
| name | text | yes | — | — |
| description | text | no | null | — |
| price | decimal(10,2) | yes | — | — |
| image_path | text | no | null | Path in menu-images storage bucket |
| available | boolean | no | true | Controls customer visibility |
| menu_variant_of | uuid | no | null | Self-reference to base item. Cascade delete. When non-null, this is a combo meal variant |

**Relationships:**
- Many menu items belong to one category
- A menu item can have one "base" item (menu_variant_of), creating a parent-child relationship for standalone vs. combo meal pricing

### 2.5 Menu Composition

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| id | uuid | auto-generated | random uuid | Primary key |
| restaurant_id | uuid | yes | — | References restaurant. Cascade delete |
| composition_type | text | yes | — | Must be one of: "accompagnement" (side), "boisson" (drink) |
| menu_item_id | uuid | yes | — | References menu item. Cascade delete |
| supplement_price | decimal(10,2) | yes | 0.00 | Extra cost on top of the combo meal base price |

**Constraints:**
- Unique combination of (restaurant_id, composition_type, menu_item_id)

**Relationships:**
- Many composition entries belong to one restaurant
- Each composition entry references one menu item (the side or drink available for combo meals)

### 2.6 Order

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| id | uuid | auto-generated | random uuid | Primary key |
| restaurant_id | uuid | yes | — | References restaurant. Cascade delete |
| table_id | uuid | yes | — | References table. Restrict delete (cannot delete table with orders) |
| status | order_status | yes | "en_attente_paiement" | See status enum below |
| total | decimal(10,2) | yes | — | Overridden server-side during checkout |
| payment_transaction_id | text | no | null | Payment provider transaction identifier |
| created_at | timestamp | auto-generated | current time | — |

**Status enum (order_status):**
| Value | Meaning |
|---|---|
| en_attente_paiement | Awaiting payment |
| paiement_echoue | Payment failed |
| nouvelle | New (paid, awaiting restaurant acceptance) |
| en_preparation | In preparation |
| prete | Ready |
| servie | Served |

**Relationships:**
- Many orders belong to one restaurant
- Many orders belong to one table
- One order has many order items

### 2.7 Order Item

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| id | uuid | auto-generated | random uuid | Primary key |
| order_id | uuid | yes | — | References order. Cascade delete |
| menu_item_id | uuid | yes | — | References menu item. Restrict delete |
| name | text | yes | — | Snapshot of item name at order time |
| quantity | integer | yes | — | Must be > 0 |
| unit_price | decimal(10,2) | yes | — | Overridden server-side with verified price |
| menu_group_id | uuid | no | null | Groups items belonging to the same combo meal |
| menu_role | text | no | null | Must be null or one of: "plat" (main), "accompagnement" (side), "boisson" (drink) |

**Relationships:**
- Many order items belong to one order
- Each order item references one menu item

### 2.8 Storage Buckets

Three public-read storage buckets for images:
- **logos**: Restaurant logo images
- **category-images**: Category header images
- **menu-images**: Menu item images

---

## 3. Roles & Permissions

### 3.1 Anonymous Customer

**How assumed:** Scans QR code at restaurant table. No account required.

**Can read:**
- Restaurants (all)
- Tables (all)
- Categories (all)
- Menu items (all, including unavailable ones — client filters by `available`)
- Menu composition (all)
- Orders (all — used for order status tracking by order ID from URL)
- Order items (all)
- Images from all storage buckets (public read)

**Can create:**
- Orders, but only with status "en_attente_paiement" or "paiement_echoue"
- Order items (no restriction on insert)

**Cannot:**
- Update or delete orders
- Update or delete order items
- Upload images to any storage bucket
- Access any admin functionality

**Authentication method:** Anonymous access via public endpoints. Identified by table URL path.

### 3.2 Restaurant Owner

**How assumed:** Signs up with email/password, then logs in. A restaurant record is linked to their user account via user_id.

**All anonymous permissions, plus:**

**Can create:**
- Their own restaurant (user_id must match authenticated user)
- Categories, menu items, menu composition for their restaurant
- Upload images to all storage buckets

**Can update:**
- Their own restaurant (name, address, logo, theme)
- Categories, menu items, menu composition for their restaurant
- Order status for their restaurant's orders

**Can delete:**
- Categories, menu items, menu composition for their restaurant

**Cannot:**
- Create or modify other restaurants' data
- Insert orders with post-payment statuses ("nouvelle", "en_preparation", "prete", "servie")
- Delete orders (no delete policy exists)
- Delete tables that have associated orders (restrict constraint)

**Authentication method:** Email/password without email confirmation.

---

## 4. User Flows

### 4.1 Customer: Browse Menu and Place Order

**Actor:** Anonymous customer
**Trigger:** Customer scans QR code at their table

**Steps:**
1. Customer scans QR code → opens URL `/menu/{restaurant-slug}/{table-id}`
2. System loads restaurant info and categories, displays category grid
3. Customer taps a category → system shows menu items for that category
4. **Branch A (standalone item):**
   - Customer taps item → modal shows item details (image, description, price)
   - Customer taps "Add" → item added to cart as standalone entry
5. **Branch B (combo meal):**
   - If category has `has_menu: true`, customer is prompted to choose "Seul" (standalone) or "Menu" (combo)
   - If "Menu" selected → 3-step stepper begins:
     - Step 1: Select main item (filtered to combo variants, `menu_variant_of` is not null)
     - Step 2: Select side (from menu composition where type = "accompagnement")
     - Step 3: Select drink (from menu composition where type = "boisson")
   - On completion, combo meal (main + side + drink) added to cart as a single grouped entry
6. Customer opens cart panel → reviews items, adjusts quantities
7. Customer taps "Commander" (order) → proceeds to checkout page
8. System creates order with status "en_attente_paiement" and inserts all order items
9. System calls server-side checkout session creation endpoint
10. Server validates order, recalculates prices from source of truth, creates payment session
11. Customer is redirected to payment provider checkout page
12. **Branch C (payment success):** Customer redirected to confirmation page, real-time order status tracking begins
13. **Branch D (payment cancelled):** Customer redirected to cancellation page, order may be deleted if still in "en_attente_paiement"

**Success outcome:** Order is created with "en_attente_paiement" status, customer completes payment, order transitions to "nouvelle".

**Failure outcomes:**
- Restaurant not found → 404 error
- Table not found → 404 error
- Menu item unavailable at checkout time → 400 error "Un article commandé n'est plus disponible"
- Order already processed → 409 error "Commande déjà traitée"
- Restaurant has no payment provider configured → 400 error "Ce restaurant n'a pas configuré les paiements en ligne"
- Payment fails at provider → order stays in "en_attente_paiement" or transitions to "paiement_echoue"
- Server-side price mismatch → total is overridden with server-calculated value

**Side effects:**
- Payment provider checkout session created
- Payment transfer to restaurant's connected account
- Real-time notification to restaurant owner's order dashboard
- Webhook received on payment completion updating order status

### 4.2 Customer: Track Order Status

**Actor:** Anonymous customer
**Trigger:** Customer lands on confirmation page after successful payment

**Steps:**
1. System loads order by ID (from URL parameter)
2. System subscribes to real-time updates on the order
3. Customer sees order timeline with current status highlighted
4. When restaurant staff updates order status, customer's view updates in real-time

**Success outcome:** Customer sees their order progress through statuses: "nouvelle" → "en_preparation" → "prete" → "servie"

**Failure outcomes:**
- Order not found → error state
- Real-time connection lost → stale data displayed (no reconnection handling documented)

**Side effects:** None (read-only operation)

### 4.3 Restaurant Owner: Sign Up and Onboard

**Actor:** New restaurant owner
**Trigger:** Owner navigates to admin signup page

**Steps:**
1. Owner enters email and password (minimum 6 characters)
2. System creates account
3. Owner logs in
4. System checks if restaurant exists for this user
5. If no restaurant exists → onboarding form shown:
   - Enter restaurant name → system generates unique slug
   - Enter number of tables → system creates table records
   - Optionally upload logo
   - Select theme
6. Owner submits → restaurant and tables created

**Success outcome:** Restaurant is created with slug, tables, and branding settings.

**Failure outcomes:**
- Email already registered → specific error message
- Password too short → specific error message
- Slug generation fails after 50 attempts → error thrown

**Side effects:** Slug auto-generated from restaurant name with uniqueness suffix

### 4.4 Restaurant Owner: Manage Menu

**Actor:** Restaurant owner
**Trigger:** Owner navigates to menu management page in admin

**Steps:**
1. System presents 4-step menu creation wizard:
   - **Step 1 — Categories:** Add categories with name, image, and has_menu toggle
   - **Step 2 — Items:** Add menu items per category with name, description, price, image, availability toggle. For categories with has_menu, also set a menu variant price.
   - **Step 3 — Accompaniments:** Configure which sides and drinks are available for combo meals, with supplement prices
   - **Step 4 — Summary:** Review all configuration before publishing
2. Owner can toggle item availability on/off at any time
3. Owner can edit or delete categories and items

**Success outcome:** Menu is fully configured and visible to customers via QR code.

**Failure outcomes:**
- Category with items cannot be meaningfully deleted (cascade deletes all items)
- Menu variant of a base item is cascade-deleted when base item is deleted

**Side effects:** None beyond database mutations

### 4.5 Restaurant Owner: Manage Orders

**Actor:** Restaurant owner
**Trigger:** Owner navigates to orders page in admin. New orders arrive in real-time.

**Steps:**
1. System loads active orders (status: "nouvelle", "en_preparation", "prete")
2. Orders displayed as cards grouped by table number
3. Owner sees order details: items, quantities, menu groups, total, table number
4. Owner taps status update button to advance order:
   - "nouvelle" → "en_preparation" (start preparing)
   - "en_preparation" → "prete" (ready for pickup)
   - "prete" → "servie" (served to customer)
5. Status update pushed in real-time to customer's tracking view

**Success outcome:** Order progresses through full lifecycle to "servie".

**Failure outcomes:**
- No specific failure handling documented for status updates

**Side effects:** Real-time update pushed to customer's order tracking page

### 4.6 Restaurant Owner: Configure Settings and Generate QR Codes

**Actor:** Restaurant owner
**Trigger:** Owner navigates to settings page in admin

**Steps:**
1. Owner updates restaurant name and/or address
2. Owner uploads or replaces logo image
3. Owner selects theme from 4 options (Classique, Chaud, Nature, Elegant)
4. Owner generates QR codes for each table — each QR code encodes the URL `/menu/{slug}/{table-id}`
5. Owner can print QR codes for placement on tables

**Success outcome:** Restaurant branding updated and QR codes generated for all tables.

**Failure outcomes:**
- Logo upload fails (storage error)
- Theme change not saved (database error)

**Side effects:** Customer-facing menu appearance changes with theme selection

---

## 5. Business Rules

### 5.1 Validation Rules

**Order creation:**
- `restaurant_id` and `table_id` are required
- `total` is required at insert but overridden server-side
- `status` must be "en_attente_paiement" or "paiement_echoue" at insert (enforced by row-level access control)
- `table_id` is NOT NULL — dine-in only (no takeaway/delivery in current version)

**Order items:**
- `quantity` must be > 0
- `unit_price` is set by client but overridden server-side with verified price
- `menu_role` must be null or one of: "plat", "accompagnement", "boisson" (database constraint)
- Items with `menu_group_id` and `menu_role` are part of a combo meal group

**Menu items:**
- `price` is required (decimal 10,2)
- `available` defaults to true
- `menu_variant_of` is a self-reference creating a combo meal variant

**Menu composition:**
- `composition_type` must be "accompagnement" or "boisson" (database constraint)
- `supplement_price` defaults to 0.00
- Unique constraint on (restaurant_id, composition_type, menu_item_id)

**Slug generation:**
- Auto-generated from restaurant name: lowercase, spaces replaced with hyphens, special characters removed
- If slug already exists, a numeric suffix is appended (up to 50 attempts)

**Signup:**
- Password minimum 6 characters
- Email must not already be registered

### 5.2 State Machine: Order Status

```
en_attente_paiement ──(payment success webhook)──→ nouvelle
en_attente_paiement ──(payment failure)──→ paiement_echoue
paiement_echoue ──(retry)──→ en_attente_paiement
nouvelle ──(restaurant staff)──→ en_preparation
en_preparation ──(restaurant staff)──→ prete
prete ──(restaurant staff)──→ servie
```

**Blocked transitions (enforced by row-level security):**
- Direct insert with "nouvelle", "en_preparation", "prete", or "servie" status → rejected
- Customer cannot update order status at all (only restaurant owner can update)
- Customer cannot delete orders (no delete policy exists)

**Status labels (French):**
| Status | Customer Label | Admin Label |
|---|---|---|
| en_attente_paiement | En attente de paiement | — |
| paiement_echoue | Paiement échoué | — |
| nouvelle | Confirmée | Nouvelle |
| en_preparation | En préparation | En préparation |
| prete | Prête | Prête |
| servie | Servie | Servie |

### 5.3 Computed Values

**Order total:**
- Initially set by the client from cart total
- Server recalculates from source-of-truth menu item prices during checkout session creation
- For combo meal sub-items (accompagnement/boisson with menu_group_id), the supplement_price from menu_composition replaces the menu item's regular price
- Server updates both the order total and individual order item unit_prices with verified values

**Cart total (client-side):**
- Standalone items: item price × quantity
- Combo meals: combo variant price (from menu_item with menu_variant_of set) × quantity
- Supplement prices are shown but already included in the combo variant price

**Supplement price display:**
- 0.00 → "Inclus" (included)
- > 0.00 → "+ X.XX €"

### 5.4 Security Constraints

**Server-side price verification:**
- All prices verified against database during checkout (not client-submitted prices)
- Prevents price manipulation by modifying client-side data
- Payment session created only after price verification passes

**Idempotent checkout:**
- Payment checkout session uses idempotency key based on order ID
- Prevents duplicate payment sessions for the same order

**Webhook signature verification:**
- Payment provider webhook verifies cryptographic signature before processing
- Rejects requests with missing or invalid signatures

**Row-level access control:**
- Every table has row-level security enabled
- Owner-scoped data access enforced at database level
- Cross-restaurant data access prevented even with direct database queries

**Order creation restrictions:**
- Only pre-payment statuses allowed on insert
- Status updates restricted to restaurant owner

### 5.5 External Integrations

**Payment processing with multi-party payouts:**
- **Capability:** Payment processing with direct transfers to restaurant accounts
- **Trigger:** After customer confirms order and enters checkout
- **Data sent:** Order items with verified prices, restaurant connected account ID
- **Data received:** Payment session URL, payment completion webhook with transaction ID
- **Failure handling:** Payment failure updates order to "paiement_echoue". Webhook signature failures return 400. Server errors during session creation return 500.

**Real-time updates:**
- **Capability:** Push-based real-time data synchronization
- **Trigger:** Order status changes (database updates)
- **Data sent:** Updated order record
- **Data received:** Updated order status on both customer and admin views
- **Failure handling:** No documented reconnection or retry mechanism

### 5.6 Error Handling

**Server-side errors (checkout session creation):**
- Missing parameters → 400 with French error message
- Restaurant not found → 404 "Restaurant introuvable"
- Restaurant has no payment account → 400 "Ce restaurant n'a pas configuré les paiements en ligne"
- Order not found or wrong restaurant → 403 "Commande invalide"
- Order already processed → 409 "Commande déjà traitée"
- Order has no items → 404 "Articles de commande introuvables"
- Invalid quantity → 400 "Quantité invalide"
- Menu item not found → 400 "Articles de menu introuvables"
- Item doesn't belong to restaurant → 403 "Article ne correspondant pas au restaurant"
- Item unavailable → 400 "Un article commandé n'est plus disponible"
- Price update failure → 500 "Erreur lors de la mise à jour du total"

**Server-side errors (webhook):**
- Missing signature → 400
- Invalid signature → 400
- Missing order ID in metadata → 400
- Database update failure → 500
- Unhandled event types → 200 (no-op, logged)

---

## 6. Edge Cases & Constraints

**Price manipulation prevention:**
- Client submits prices with the order, but the server completely recalculates from database-stored prices during checkout session creation. Both order total and individual item prices are overwritten. (Validated in checkout session integration tests.)

**Combo meal supplement pricing:**
- For combo meal sub-items (sides/drinks), the supplement_price from menu_composition replaces the regular menu item price in the total calculation, not added on top. This means a side that costs 3.50 standalone but has supplement_price 0.50 in a combo contributes only 0.50 to the combo total. (Derived from checkout session logic.)

**Order item availability race condition:**
- An item could be marked unavailable between the time a customer adds it to cart and checkout. The server checks availability during checkout session creation and returns an error if any item is unavailable. No soft reservation mechanism exists.

**Table deletion restriction:**
- Tables with existing orders cannot be deleted (ON DELETE RESTRICT). This prevents data integrity issues with historical orders.

**Cascade deletes:**
- Deleting a restaurant cascades to all related data (categories, items, orders, etc.)
- Deleting a category cascades to its menu items and their composition entries
- Deleting a base menu item cascades to its combo variant (menu_variant_of pointing to it)

**Cart entry uniqueness:**
- Each cart entry has a unique ID combining the item ID and (for combo meals) the selected side and drink IDs. This allows the same main item to appear multiple times with different combo selections.

**Theme system:**
- Four predefined themes control the customer-facing menu appearance via CSS custom properties. Themes are stored as a string on the restaurant and applied via a data attribute on the root element.

**Real-time subscription scope:**
- Customer order tracking subscribes to UPDATE events filtered by order ID
- Admin order dashboard subscribes to INSERT and UPDATE events for all restaurant orders
- No real-time subscriptions for categories, menu items, or menu composition — these require manual refresh

**Slug generation retries:**
- If a slug already exists, the system appends a numeric suffix and retries up to 50 times before throwing an error.

**QR code URL structure:**
- Format: `/menu/{restaurant-slug}/{table-id}` — the slug uniquely identifies the restaurant, and the table ID identifies the specific table for order attribution.

**Dine-in only:**
- The system currently only supports dine-in orders. `table_id` is required (NOT NULL) on orders. No takeaway or delivery functionality exists.

---

## 7. Gaps & Assumptions

### 7.1 Missing: Payment failure webhook handling

**What is missing:** The webhook handler only processes `checkout.session.completed` events. There is no explicit handler for `checkout.session.expired` or payment failure events from the payment provider.
**Where found:** `supabase/functions/stripe-webhook/index.ts` — the switch statement only handles one event type; others are logged but not acted upon.
**Assumption:** Payment failures may be handled by the payment provider's redirect (cancel_url) and the client-side cancellation page logic, but the server does not proactively update the order status to "paiement_echoue" via webhook. The order could remain in "en_attente_paiement" indefinitely if the customer abandons payment without reaching the cancel URL.
**Flag for human review:** Confirm whether abandoned payments should have a cleanup mechanism (e.g., scheduled job or additional webhook handler).

### 7.2 Missing: Order item validation during insert

**What is missing:** The row-level security policy for order_items insert uses `with check (true)`, meaning anyone can insert any order item, including items from other restaurants or items that don't exist.
**Where found:** `supabase/migrations/001_initial_schema.sql` line 186.
**Assumption:** The server-side checkout session creation validates items during payment, but there's no database-level protection against inserting invalid order items. A malicious client could insert order items referencing non-existent or wrong-restaurant menu items before checkout.
**Flag for human review:** Consider adding a row-level security check on order_items insert to validate menu_item_id belongs to the correct restaurant.

### 7.3 Missing: Storage update/delete policy enforcement

**What is missing:** The storage update policies do not verify that the authenticated user owns the restaurant. The policy checks only bucket_id, not whether the file belongs to the user's restaurant.
**Where found:** `supabase/migrations/20260503191836_storage_update_policies.sql` — policies only check `bucket_id`, not user ownership.
**Assumption:** Any authenticated restaurant owner can update images in any restaurant's storage path. This is a security gap but may be acceptable for a single-tenant or trusted environment.
**Flag for human review:** Add ownership verification to storage update/delete policies.

### 7.4 Missing: Concurrent order handling

**What is missing:** No mechanism to prevent the same customer from creating multiple orders simultaneously (e.g., opening the checkout page in multiple tabs).
**Where found:** Not present anywhere in the codebase.
**Assumption:** The idempotency key on payment sessions prevents duplicate charges, but multiple orders with "en_attente_paiement" status could be created.
**Flag for human review:** Consider whether this is a concern for the MVP.

### 7.5 Missing: Real-time reconnection handling

**What is missing:** No documented reconnection or retry logic for real-time subscriptions. If the connection drops, the client may display stale data.
**Where found:** Not present in client hooks (`useOrderStatus`, `useOrders`).
**Assumption:** The real-time library handles reconnection internally, but this has not been explicitly coded for.
**Flag for human review:** Verify real-time library reconnection behavior and add manual handling if needed.

### 7.6 Missing: Category and menu item ordering enforcement

**What is missing:** While `position` field exists on categories, there's no constraint preventing duplicate position values or gaps.
**Where found:** `supabase/migrations/001_initial_schema.sql` — `position int not null default 0` with no unique constraint.
**Assumption:** Categories are sorted by position, but duplicate positions may cause unpredictable ordering.
**Flag for human review:** Low priority — admin can manually adjust positions.

### 7.7 Missing: Restaurant deletion cascade implications

**What is missing:** Deleting a restaurant cascades to all related data including orders with payment records. This could cause issues with payment provider reconciliation.
**Where found:** `supabase/migrations/001_initial_schema.sql` — `on delete cascade` on all restaurant-related tables.
**Assumption:** Restaurant deletion is an admin-only operation not currently exposed in the UI. The cascade behavior is the default and may need adjustment for production.
**Flag for human review:** Confirm whether orders should be soft-deleted or archived instead of cascade-deleted.

### 7.8 Missing: Menu variant creation logic

**What is missing:** The relationship between base items and menu variants (menu_variant_of) requires manual coordination. When creating a new item in a has_menu category, both the base item and its menu variant must be created separately. The admin UI handles this in StepItems, but there's no database constraint ensuring variants exist for has_menu categories.
**Where found:** Admin menu management components.
**Assumption:** The admin wizard guides the owner through creating both, but incomplete states are possible (category with has_menu=true but no variants).
**Flag for human review:** Consider validation to ensure menu variants exist for has_menu categories.

### 7.9 Missing: Customer cancellation flow

**What is missing:** The cancellation page allows deleting an order in "en_attente_paiement" status. However, there is no delete policy on orders in the row-level security. The comment in the migration says "customers should not be able to cancel any pending order."
**Where found:** `supabase/migrations/001_initial_schema.sql` line 169 comment, and `client/src/pages/CancelledPage/CancelledPage.tsx`.
**Assumption:** The cancellation page attempts to delete the order, but this may fail due to the missing delete policy. The "cancel" flow may actually be a no-op or error that the UI doesn't handle gracefully.
**Flag for human review:** Verify the cancellation flow works end-to-end and clarify the intended behavior.

---

## 8. Behavioral Scenarios

### @ordering
Feature: Customer places a standalone order
  A customer scans a QR code, selects items, and pays for their order.

  @customer
  Scenario: Successful standalone item order
    Given a restaurant "Naia Burger" with slug "naia-burger" exists
    And table 5 exists for restaurant "naia-burger"
    And category "Burgers" exists with has_menu false
    And menu item "Burger classique" priced at 6.90 is available in "Burgers"
    When the customer opens the menu URL for restaurant "naia-burger" table 5
    And the customer taps category "Burgers"
    And the customer taps item "Burger classique"
    And the customer adds the item to cart
    And the customer proceeds to checkout
    And the customer completes payment
    Then an order is created with status "nouvelle"
    And the order contains 1 item "Burger classique" with quantity 1 and unit_price 6.90
    And the order total is 6.90
    And the payment provider records a transaction

  @customer
  Scenario: Successful multiple item order
    Given a restaurant "Naia Burger" with slug "naia-burger" exists
    And table 3 exists for restaurant "naia-burger"
    And category "Burgers" exists with has_menu false
    And menu item "Burger classique" priced at 6.90 is available in "Burgers"
    And menu item "Burger bacon" priced at 8.50 is available in "Burgers"
    When the customer adds 2 of "Burger classique" to cart
    And the customer adds 1 of "Burger bacon" to cart
    And the customer proceeds to checkout and completes payment
    Then the order total is 22.30
    And the order contains "Burger classique" with quantity 2
    And the order contains "Burger bacon" with quantity 1

  @customer @edge-case
  Scenario: Item becomes unavailable between cart and checkout
    Given a restaurant "Naia Burger" exists with table 1
    And menu item "Burger classique" priced at 6.90 is available
    And the customer has "Burger classique" in cart
    When the restaurant owner marks "Burger classique" as unavailable
    And the customer proceeds to checkout
    Then the checkout fails with error "Un article commandé n'est plus disponible"
    And no order is created with the payment provider

  @customer @edge-case
  Scenario: Price mismatch between client and server
    Given a restaurant "Naia Burger" exists with table 1
    And menu item "Burger classique" priced at 6.90 is available
    And the customer submits an order with total 1.00 (tampered price)
    When the checkout session is created
    Then the order total is corrected to 6.90
    And the order item unit_price is corrected to 6.90
    And the payment session uses the verified price 6.90

### @ordering
Feature: Customer places a combo meal order
  A customer orders a menu (main + side + drink) through a 3-step stepper.

  @customer
  Scenario: Successful combo meal order
    Given a restaurant "Naia Burger" exists with table 2
    And category "Burgers" exists with has_menu true
    And base item "Burger classique" priced at 6.90 exists
    And menu variant "Menu Burger classique" priced at 10.40 exists for "Burger classique"
    And side "Frites" with supplement_price 0.00 is available for combo meals
    And side "Nuggets x6" with supplement_price 1.50 is available for combo meals
    And drink "Coca-Cola" with supplement_price 0.00 is available for combo meals
    When the customer selects category "Burgers"
    And the customer chooses "Menu" mode
    And the customer selects main "Menu Burger classique"
    And the customer selects side "Nuggets x6" (supplement +1.50)
    And the customer selects drink "Coca-Cola" (included)
    And the customer completes the combo and proceeds to checkout
    Then the cart contains a combo meal entry with total 10.40
    And after checkout, the order contains 3 items grouped by menu_group_id:
      | name                  | menu_role        | unit_price |
      | Menu Burger classique | plat             | 10.40      |
      | Nuggets x6            | accompagnement   | 1.50       |
      | Coca-Cola             | boisson          | 0.00       |

  @customer
  Scenario: Customer adds two different combos with same main
    Given a restaurant with combo meals configured
    When the customer adds a combo with main "Menu Burger classique", side "Frites", drink "Coca-Cola"
    And the customer adds another combo with main "Menu Burger classique", side "Nuggets", drink "Fanta"
    Then the cart contains 2 separate combo entries
    And each entry has a unique cart entry ID

  @customer @edge-case
  Scenario: Customer cannot skip steps in combo stepper
    Given the customer is on step 1 (main selection) of the combo stepper
    Then the customer cannot navigate to step 2 (side selection)
    When the customer selects a main item
    Then the customer can navigate to step 2

### @ordering
Feature: Payment flow
  The checkout session creation validates and processes payment.

  @customer @edge-case
  Scenario: Order already processed when checkout is attempted
    Given an order exists with status "nouvelle"
    When the customer attempts to create a checkout session for this order
    Then the response is 409 with error "Commande déjà traitée"

  @customer @edge-case
  Scenario: Restaurant has no payment account configured
    Given a restaurant exists without a payment_provider_account_id
    And an order exists with status "en_attente_paiement"
    When the customer attempts to create a checkout session
    Then the response is 400 with error "Ce restaurant n'a pas configuré les paiements en ligne"

  @customer @edge-case
  Scenario: Missing required parameters for checkout
    When a checkout session request is sent without orderId
    Then the response is 400 with error "orderId and restaurantId are required"

  @customer @edge-case
  Scenario: Order belongs to a different restaurant
    Given restaurant A and restaurant B both exist
    And an order exists for restaurant A with status "en_attente_paiement"
    When a checkout session is requested with orderId for A but restaurantId for B
    Then the response is 403 with error "Commande invalide"

  @customer @edge-case
  Scenario: Order has zero items
    Given an order exists with status "en_attente_paiement" but no order items
    When a checkout session is created
    Then the response is 404 with error "Articles de commande introuvables"

### @ordering
Feature: Webhook payment processing
  The payment provider sends webhook events to update order status.

  @customer @edge-case
  Scenario: Successful payment webhook
    Given an order exists with status "en_attente_paiement"
    When a payment completion webhook is received with valid signature
    Then the order status is updated to "nouvelle"
    And the order's payment_transaction_id is set

  @customer @edge-case
  Scenario: Webhook with missing signature
    When a webhook request arrives without a signature header
    Then the response is 400

  @customer @edge-case
  Scenario: Webhook with invalid signature
    When a webhook request arrives with an invalid signature
    Then the response is 400 with error "Webhook Error"

  @customer @edge-case
  Scenario: Webhook for unhandled event type
    When a webhook request arrives for event type "payment_intent.created" with valid signature
    Then the response is 200
    And no order status change occurs

### @ordering
Feature: Order cancellation page
  Customer lands on cancellation page after abandoning payment.

  @customer @assumption
  Scenario: Customer cancels payment for pending order
    Given an order exists with status "en_attente_paiement"
    When the customer lands on the cancellation page
    Then the system attempts to delete the order
    And the customer sees a cancellation confirmation

  @customer @edge-case
  Scenario: Customer lands on cancellation page for already-processed order
    Given an order exists with status "nouvelle"
    When the customer lands on the cancellation page
    Then the order is not deleted

### @ordering
Feature: Customer order status tracking
  Customer tracks their order in real-time after payment.

  @customer
  Scenario: Customer sees order progress through all statuses
    Given an order exists with status "nouvelle"
    When the restaurant owner updates status to "en_preparation"
    Then the customer sees "En préparation" without page refresh
    When the restaurant owner updates status to "prete"
    Then the customer sees "Prête" without page refresh
    When the restaurant owner updates status to "servie"
    Then the customer sees "Servie" without page refresh

### @admin
Feature: Restaurant owner signup and onboarding
  New restaurant owners create their account and set up their restaurant.

  @owner
  Scenario: Successful signup and restaurant creation
    Given no account exists for "owner@example.com"
    When the owner signs up with email "owner@example.com" and password "Secret123!"
    Then an account is created
    And the owner is prompted to create a restaurant
    When the owner enters restaurant name "My Burger" and 5 tables
    Then a restaurant is created with slug "my-burger"
    And 5 table records are created

  @owner @edge-case
  Scenario: Signup with already registered email
    Given an account exists for "owner@example.com"
    When a new user signs up with email "owner@example.com"
    Then a specific error message about already registered email is shown

  @owner @edge-case
  Scenario: Signup with short password
    When a user signs up with password "abc" (less than 6 characters)
    Then a specific error about password length is shown

  @owner @edge-case
  Scenario: Slug collision during restaurant creation
    Given a restaurant with slug "my-burger" already exists
    When a new owner creates a restaurant named "My Burger"
    Then the slug "my-burger-1" is generated

### @admin
Feature: Restaurant owner manages menu
  Restaurant owners create and edit their menu categories, items, and compositions.

  @owner
  Scenario: Create a new category with image
    Given the owner is authenticated
    When the owner creates category "Pizzas" with image and has_menu false
    Then the category is saved with the uploaded image
    And the category is visible to customers

  @owner
  Scenario: Create a menu item with combo variant
    Given the owner is authenticated and category "Burgers" has has_menu true
    When the owner creates item "Burger classique" with price 6.90
    And sets menu price to 10.40
    Then a base item is created with price 6.90
    And a menu variant is created with price 10.40 linked to the base item

  @owner
  Scenario: Toggle item availability
    Given menu item "Burger classique" is available
    When the owner toggles availability off
    Then "Burger classique" is no longer shown to customers
    And if it was in a customer's cart, checkout will fail with availability error

  @owner
  Scenario: Delete base item cascades to variant
    Given base item "Burger classique" exists with menu variant "Menu Burger classique"
    When the owner deletes "Burger classique"
    Then "Menu Burger classique" is also deleted

  @owner
  Scenario: Configure combo meal composition
    Given the owner is authenticated
    When the owner adds "Frites" as an accompagnement with supplement_price 0.00
    And adds "Nuggets x6" as an accompagnement with supplement_price 1.50
    And adds "Coca-Cola" as a boisson with supplement_price 0.00
    Then these items are available for combo meal selection
    And "Frites" shows as "Inclus"
    And "Nuggets x6" shows as "+ 1.50 €"

  @owner @edge-case
  Scenario: Cannot link composition item from another restaurant
    Given restaurant A has item "Frites" and restaurant B exists
    When the owner of restaurant B tries to add "Frites" (from restaurant A) as a composition item
    Then the insert is rejected by row-level security

### @admin
Feature: Restaurant owner manages orders
  Restaurant owners view and update order statuses in real-time.

  @owner
  Scenario: Owner sees new order arrive in real-time
    Given the owner is on the orders page
    When a customer places and pays for an order at table 3
    Then the order appears on the owner's orders page immediately
    And the order shows status "Nouvelle" and table number 3

  @owner
  Scenario: Owner advances order through full lifecycle
    Given an order exists with status "nouvelle" at table 1
    When the owner taps "En préparation"
    Then the order status becomes "en_preparation"
    And the customer sees the status update in real-time
    When the owner taps "Prête"
    Then the order status becomes "prete"
    When the owner taps "Servie"
    Then the order status becomes "servie"
    And the order is no longer shown in active orders

  @owner @edge-case
  Scenario: Owner cannot update another restaurant's orders
    Given restaurant A has an order
    And the owner of restaurant B is authenticated
    When the owner of B tries to update the order status
    Then the update is rejected by row-level security

### @admin
Feature: Restaurant owner configures settings
  Restaurant owners update branding and generate QR codes.

  @owner
  Scenario: Owner updates restaurant theme
    Given the owner's restaurant uses theme "classique"
    When the owner selects theme "chaud"
    Then the customer-facing menu uses warm color scheme

  @owner
  Scenario: Owner generates QR codes for all tables
    Given the restaurant has 9 tables
    When the owner generates QR codes
    Then 9 QR codes are generated, each encoding the URL `/menu/{slug}/{table-id}`

  @owner
  Scenario: Owner uploads new logo
    Given the owner is authenticated
    When the owner uploads a new logo image
    Then the logo is stored in the logos bucket
    And the restaurant's logo_path is updated

### @access-control
Feature: Row-level access control enforcement
  Data access is strictly scoped by restaurant ownership.

  @owner @edge-case
  Scenario: Owner cannot update another restaurant
    Given restaurant A is owned by owner X and restaurant B is owned by owner Y
    When owner X tries to update restaurant B's name
    Then the update is rejected

  @owner @edge-case
  Scenario: Owner cannot modify another restaurant's categories
    Given owner X tries to insert a category for restaurant B
    Then the insert is rejected

  @owner @edge-case
  Scenario: Owner cannot modify another restaurant's menu items
    Given owner X tries to update a menu item belonging to restaurant B
    Then the update is rejected

  @customer @edge-case
  Scenario: Anonymous user cannot upload images
    When an unauthenticated user tries to upload to the logos bucket
    Then the upload is rejected

  @customer @edge-case
  Scenario: Anonymous user cannot insert order with post-payment status
    When an anonymous user tries to insert an order with status "nouvelle"
    Then the insert is rejected

  Scenario Outline: Order status insert restrictions
    Given an anonymous user attempts to create an order
    When the order status is "<status>"
    Then the insert is "<result>"
    Examples:
      | status               | result    |
      | en_attente_paiement  | allowed   |
      | paiement_echoue      | allowed   |
      | nouvelle             | rejected  |
      | en_preparation       | rejected  |
      | prete                | rejected  |
      | servie               | rejected  |

### @data-integrity
Feature: Data integrity constraints
  Database constraints ensure data consistency.

  @edge-case
  Scenario: Table with orders cannot be deleted
    Given table 5 has an associated order
    When a delete is attempted on table 5
    Then the delete is rejected by the restrict constraint

  @edge-case
  Scenario: Menu composition unique constraint
    Given restaurant "Naia Burger" has item "Frites" as accompagnement
    When a duplicate entry is inserted for the same restaurant, type, and item
    Then the insert is rejected by the unique constraint

  @edge-case
  Scenario: Menu role constraint on order items
    When an order item is inserted with menu_role "invalid_role"
    Then the insert is rejected by the check constraint

  Scenario Outline: Menu role valid values
    Given an order item with menu_group_id set
    When menu_role is "<role>"
    Then the insert is "<result>"
    Examples:
      | role            | result   |
      | plat            | allowed  |
      | accompagnement  | allowed  |
      | boisson         | allowed  |
      | null            | allowed  |
      | dessert         | rejected |

  @edge-case
  Scenario: Slug uniqueness enforced
    Given a restaurant with slug "naia-burger" exists
    When a new restaurant is created with slug "naia-burger"
    Then the insert is rejected

  @edge-case
  Scenario: Table number unique per restaurant
    Given table number 5 exists for restaurant "Naia Burger"
    When another table with number 5 is created for the same restaurant
    Then the insert is rejected
