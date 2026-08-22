# Going live: from localhost to a public domain

This assumes you're deploying on the same EC2 box you've been testing on. It adds a
Caddy reverse proxy (`infra/docker-compose.prod.yml`) so the whole app sits behind one
public HTTPS domain instead of raw container ports.

## 1. Get a domain pointed at your instance

- Buy/use any domain (Route 53, Namecheap, whatever).
- Find your EC2 instance's **public IP** (or better, allocate an **Elastic IP** so it
  doesn't change on reboot: EC2 console → Elastic IPs → Allocate → Associate with your
  instance).
- Create an **A record** for your domain (e.g. `chaipass.yourdomain.com`) pointing at
  that public IP.
- Wait for DNS to propagate (`dig chaipass.yourdomain.com` should return your IP —
  usually a few minutes, occasionally up to an hour).

## 2. Open the right ports in your EC2 security group

In the AWS console → EC2 → your instance → Security tab → security group → Edit inbound rules:

| Type | Port | Source | Why |
|---|---|---|---|
| HTTP | 80 | 0.0.0.0/0 | Caddy needs this for the Let's Encrypt HTTP-01 challenge, and to redirect to HTTPS |
| HTTPS | 443 | 0.0.0.0/0 | The actual live traffic |
| SSH | 22 | your IP only | Keep this locked to your IP, not 0.0.0.0/0 |

**Remove/don't add public rules for 4000 or 5173** — once `docker-compose.prod.yml` is
applied, those ports are no longer published to the host at all (Caddy is the only
public entry point; the gateway and frontend containers are reachable only from Caddy
over the internal Docker network).

## 3. Point the Caddyfile at your real domain

Edit `infra/Caddyfile` and replace the placeholder:
```
your-domain.com {
```
with your actual domain, e.g.:
```
chaipass.yourdomain.com {
```

## 4. Bring it up with the production overlay

```bash
cd ~/chai/infra
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

What this changes versus your current local setup:
- Adds a `caddy` container publishing 80/443, reverse-proxying `/api/*` and
  `/socket.io/*` to the gateway and everything else to the frontend, and automatically
  requesting + renewing a Let's Encrypt certificate for your domain.
- Stops publishing `gateway:4000` and `frontend:5173` directly to the host.
- Rebuilds the frontend with `VITE_API_URL=/api` (a relative path), since the frontend
  and API are now served from the same origin through Caddy — this also means you no
  longer need CORS configured for cross-origin browser calls.

## 5. Verify

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
curl -I https://chaipass.yourdomain.com
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs caddy
```
The `caddy` logs should show it obtaining a certificate on first start. Visiting
`https://your-domain.com` in a browser should show the app with a valid padlock.

## 6. Update `.env` for production values

Your existing `infra/.env` still needs real secrets (not the placeholder ones from
`.env.example`):
```bash
nano ~/chai/infra/.env
```
- Generate strong random values for `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`,
  `SERVICE_JWT_SECRET`, `QR_JWT_SECRET`: `openssl rand -hex 32` for each.
- Set `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` / `RAZORPAY_WEBHOOK_SECRET` to your real
  (or test-mode) Razorpay dashboard values.
- `CORS_ORIGIN` can stay as-is; it's now a fallback rather than load-bearing since
  frontend↔gateway calls are same-origin through Caddy.
- In the Razorpay dashboard, set the webhook URL to
  `https://chaipass.yourdomain.com/api/payments/webhook`.

After changing `.env`, restart so the new values take effect:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

## 7. Ongoing operations

- **Logs:** `docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f <service>`
- **Restart one service:** `docker compose -f docker-compose.yml -f docker-compose.prod.yml restart gateway`
- **Redeploy after a code change:** `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build`
- **Certificate renewal:** automatic — Caddy handles this itself, nothing to schedule.
- **Mongo backups:** this setup runs a single-node Mongo for simplicity. Before real
  customer data goes in, either move to MongoDB Atlas (managed, backed up, and
  matches the `docs/ARCHITECTURE.md` production target) or set up a cron `mongodump`
  to S3 — ask me and I'll wire either one up.

## What's still "local machine" about this setup

Running everything on one EC2 box is fine to get live and demo real traffic, but it's a
single point of failure and the Mongo replica set is single-node (fine for
transactions, not for durability). `docs/ARCHITECTURE.md`'s AWS section describes the
next step up — ECS Fargate for gateway/business-service, S3+CloudFront for the
frontend, MongoDB Atlas — for whenever you're ready to move off a single instance.
