# customer-app

The Stage 18 frontend — one React app covering all 4 of Vicinia's roles,
role-gated by route rather than split into separate apps, since they all
share the same auth system and design language:

- **Customer** (`/`, `/search`, `/product/:id`, `/cart`, `/checkout`,
  `/orders`, `/wallet`, `/profile`, `/addresses`) — browse, cart, checkout
  (wallet or Razorpay test mode), order tracking, reviews.
- **Merchant** (`/merchant/*`) — apply/onboarding, store profile + hours,
  listings (create by searching the catalog or requesting a new product),
  order queue, settlements.
- **Delivery partner** (`/delivery/*`) — online/offline + live location
  ping, per-task accept/picked-up/delivered actions.
- **Admin** (`/admin/*`) — merchant approvals, catalog moderation
  (product review, categories), coupons, settlement/payout jobs.

Blinkit-inspired visual language (bold yellow accent, near-black text,
rounded cards) as a proper wide web app, not a phone frame — see
`src/styles/tokens.css` for the palette (three-state light/dark theming)
and `src/styles/app.css` for everything built on it. `src/components/Icons.jsx`
and `Illustrations.jsx` are the whole app's icon/illustration set — hand-drawn
SVG, no icon library.

## Run it

Backend must be up first (`../../start-infra.sh` from the repo root), then:

```
npm install       # first time only
npm run dev        # http://localhost:5173
```

`.env` points at the gateway (`VITE_API_BASE_URL`, default `http://localhost:8080`)
— copy `.env.example` if you need to change it. The gateway's CORS config
(`api-gateway/src/main/resources/application.yml`) is locked to
`http://localhost:5173` specifically — a different dev port needs a
matching change there.

## How auth & roles work here

- Tokens and the logged-in user's `roles`/`permissions` live in `localStorage`
  under one JSON blob (`src/api/storage.js`).
- `src/api/client.js`'s response interceptor transparently refreshes an
  expired access token on a 401 and retries the original request once —
  concurrent 401s are deduped into a single `/refresh` call.
- `src/utils/roles.js` + `src/components/RoleRoute.jsx` gate the merchant/
  delivery/admin route trees by the account's role, redirecting a logged-in
  user who hits another role's area to their own home instead of a 403 —
  since admin promotion (see the repo root README's SQL workaround) strips
  the CUSTOMER role off the account it's granted to, in practice every
  account carries exactly one meaningful role.

## A known, honest backend gap

delivery-service has no "list my assigned tasks" endpoint, and
notification-service has no delivery-assignment consumer — so a delivery
partner has no server-side signal that a task exists. `DeliveryHomePage`
is built around what's actually there (online/offline, location ping,
and per-task actions reachable by order ID) rather than faking a task
inbox; see `src/utils/deliveryHistory.js`'s comment for the full story.

## Structure

```
src/
  api/            axios client + one wrapper module per backend service
  context/        AuthContext (logged-in user), CartContext (live cart state)
  components/     shared UI: Navbar, StatusBadge, ProductImage, Icons, Illustrations, modals
  pages/          customer pages at the top level; merchant/, delivery/, admin/ subfolders
  utils/          roles.js, format.js, status.js, deliveryHistory.js
  styles/         tokens.css (design tokens) + app.css (everything else)
```
