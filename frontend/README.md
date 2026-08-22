# ChaiPass Frontend

This folder is the React SPA scaffold referenced in the main README/ARCHITECTURE docs
(Vite + React Router + role-based dashboards, talking only to the Node gateway).

For an immediate, fully clickable preview of the intended premium UI (no build step
required), open `public/demo.html` directly in a browser — it's the same design
language (tea green / brown / cream / gold accents, Fraunces + Manrope type) implemented
as a static, self-contained mock with in-memory interactivity across all three
dashboards (Customer, Shop owner, Admin).

Wiring `demo.html`'s screens into real Vite/React components that call the gateway's
`/api/*` routes (see `docs/API.md`) is the next build step once `npm create vite@latest`
is run in this folder.
