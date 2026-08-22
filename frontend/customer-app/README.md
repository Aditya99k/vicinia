# customer-app

A real React app to click through and validate the backend built so far
(Stage 0-2: auth-service, user-service, api-gateway) — **not** the full
Stage 18 customer app. It only covers what those services actually
support today: signup, login, forgot/reset password, profile, and
addresses. Merchant browsing, cart, checkout, and everything else shows
as an honest "coming soon" placeholder rather than being faked.

Blinkit-inspired visual language (bold yellow accent, near-black text,
rounded cards, mobile-first phone-frame layout) — see `src/styles/tokens.css`
for the palette and `src/styles/app.css` for everything built on it.

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

## How auth actually works here

- Tokens live in `localStorage` under one JSON blob (`src/api/storage.js`) —
  fine for a local validation tool, not how the real Stage 18 app should
  handle it.
- `src/api/client.js`'s response interceptor transparently refreshes an
  expired access token on a 401 and retries the original request once —
  concurrent 401s are deduped into a single `/refresh` call. This is the
  actual refresh-token rotation flow from `docs/ARCHITECTURE.md` §14
  exercised for real, not mocked.
- Logging out, letting the 15-minute access token expire, or clicking
  "Forgot password?" (which reads the reset token straight out of
  `logs/auth-service.log`, since notification-service doesn't exist
  until Stage 12) all exercise real backend behavior end to end.

## Structure

```
src/
  api/        axios client + auth/user endpoint wrappers
  context/    AuthContext — holds the logged-in user, exposes signup/login/logout
  components/ TopBar, BottomNav, AppLayout, AddressCard, AddressFormModal, icons
  pages/      Login, Signup, Home, Profile, Addresses
  styles/     tokens.css (design tokens) + app.css (everything else)
```
