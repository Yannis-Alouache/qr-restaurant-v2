/**
 * Jeu de démo pour la vue Commandes (dev uniquement) :
 * crée des commandes via le même parcours que les e2e
 * (POST /api/public/orders -> webhook Stripe signé -> PATCH statut admin).
 * Les menus sont commandés en groupe de 3 (plat/accompagnement/boisson,
 * même menuGroupId, même quantité) comme l'exige OrderPricingPolicy.
 *
 * Les temps d'attente affichés se règlent ensuite en base (les tags du PLAN
 * donnent l'offset voulu), par ex. :
 *   docker exec qr-restaurant-db sh -c "psql -U \$POSTGRES_USER -d qr_restaurant_e2e \
 *     -c \"UPDATE order_table SET created_at = now() - interval '12 minutes' WHERE id = '<id>'\""
 * (la base visée est celle de l'API qui répond sur 8080 — vérifier avec le log Spring).
 */
const API = 'http://localhost:8080';

const ITEMS = {
  burgerClassique: 'e0eebc99-0001-4ef8-bb6d-6bb9bd380a01',
  burgerBacon: 'e0eebc99-0001-4ef8-bb6d-6bb9bd380a02',
  brownie: 'e0eebc99-0001-4ef8-bb6d-6bb9bd380a03',
  menuClassique: 'e0eebc99-0002-4ef8-bb6d-6bb9bd380a01',
  menuBacon: 'e0eebc99-0002-4ef8-bb6d-6bb9bd380a02',
  frites: 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a01',
  nuggets: 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a02',
  coca: 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a03',
  fanta: 'f0eebc99-0001-4ef8-bb6d-6bb9bd380a04',
};

const T1 = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01';
const T2 = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02';
const T3 = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03';

// Un item : [id, qty] ou groupe de menu { group: [plat, accompagnement, boisson], qty }.
const PLAN = [
  {
    tag: 'nouvelle-12min', table: T2, status: 'nouvelle',
    items: [[ITEMS.burgerBacon, 2], [ITEMS.frites, 1], [ITEMS.coca, 1]],
  },
  {
    tag: 'nouvelle-fresh', table: T3, status: 'nouvelle',
    items: [
      { group: [ITEMS.menuBacon, ITEMS.frites, ITEMS.fanta], qty: 1 },
      [ITEMS.brownie, 1],
    ],
  },
  {
    tag: 'attente-paiement', table: T1, status: null,
    items: [[ITEMS.burgerClassique, 1], [ITEMS.coca, 1]],
  },
  {
    tag: 'preparation-6min', table: T3, status: 'en_preparation',
    items: [{ group: [ITEMS.menuClassique, ITEMS.nuggets, ITEMS.coca], qty: 2 }],
  },
  {
    tag: 'preparation-9min', table: T2, status: 'en_preparation',
    items: [[ITEMS.burgerBacon, 1], [ITEMS.frites, 2]],
  },
  {
    tag: 'prete-3min', table: T1, status: 'prete',
    items: [[ITEMS.burgerClassique, 3], [ITEMS.fanta, 2]],
  },
  {
    tag: 'servie', table: T2, status: 'servie',
    items: [[ITEMS.brownie, 2], [ITEMS.coca, 2]],
  },
];

function toRequestItems(spec) {
  const rows = [];
  for (const item of spec.items) {
    if (Array.isArray(item)) {
      rows.push({ menuItemId: item[0], quantity: item[1] });
    } else {
      const menuGroupId = crypto.randomUUID();
      const [plat, accompagnement, boisson] = item.group;
      rows.push(
        { menuItemId: plat, quantity: item.qty, menuGroupId, menuRole: 'plat' },
        { menuItemId: accompagnement, quantity: item.qty, menuGroupId, menuRole: 'accompagnement' },
        { menuItemId: boisson, quantity: item.qty, menuGroupId, menuRole: 'boisson' },
      );
    }
  }
  return rows;
}

async function main() {
  const login = await fetch(`${API}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'owner@test.com', password: 'Secret123!' }),
  });
  if (!login.ok) throw new Error(`login ${login.status}: ${await login.text()}`);
  // L'API authentifie via cookie httpOnly « jwt » (pas de token dans le corps).
  const jwt = (login.headers.get('set-cookie') ?? '').split(';')[0];
  const auth = { Cookie: jwt };

  let n = 0;
  for (const spec of PLAN) {
    n += 1;
    const create = await fetch(`${API}/api/public/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ slug: 'naia-burger', tableId: spec.table, items: toRequestItems(spec) }),
    });
    if (!create.ok) throw new Error(`[${spec.tag}] create ${create.status}: ${await create.text()}`);
    const { id } = await create.json();

    if (spec.status !== null) {
      // Payer via le webhook Stripe signé (même recette que e2e/support/api.ts).
      const payload = JSON.stringify({
        id: `evt_seed_${n}_${Date.now()}`,
        object: 'event',
        api_version: '2025-04-30.basil',
        type: 'checkout.session.completed',
        data: { object: {
          id: `cs_seed_${n}_${Date.now()}`,
          object: 'checkout.session',
          payment_intent: `pi_seed_${n}_${Date.now()}`,
          metadata: { order_id: id },
        } },
      });
      const timestamp = Math.floor(Date.now() / 1000);
      const { createHmac } = await import('node:crypto');
      const signature = createHmac('sha256', 'whsec_test').update(`${timestamp}.${payload}`).digest('hex');
      const hook = await fetch(`${API}/api/webhooks/stripe`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Stripe-Signature': `t=${timestamp},v1=${signature}` },
        body: payload,
      });
      if (!hook.ok) throw new Error(`[${spec.tag}] webhook ${hook.status}: ${await hook.text()}`);

      if (spec.status !== 'nouvelle') {
        // La machine à états interdit les sauts : on suit le chemin complet.
        const path = { en_preparation: ['en_preparation'], prete: ['en_preparation', 'prete'], servie: ['en_preparation', 'prete', 'servie'] }[spec.status];
        for (const status of path) {
          const patch = await fetch(`${API}/api/admin/orders/${id}/status`, {
            method: 'PATCH',
            headers: { ...auth, 'Content-Type': 'application/json' },
            body: JSON.stringify({ status }),
          });
          if (!patch.ok) throw new Error(`[${spec.tag}] patch ${status} ${patch.status}: ${await patch.text()}`);
        }
      }
    }
    console.log(`${spec.tag}\t${id}\tstatus=${spec.status ?? 'en_attente_paiement'}`);
  }
  console.log('DONE');
}

main().catch((err) => { console.error(err.message); process.exit(1); });
