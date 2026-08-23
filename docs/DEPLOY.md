# Going live: from localhost to a public domain

The stack has one base file plus two mutually-exclusive overlays — pick whichever
matches what you're doing right now:

| Command | When to use it |
|---|---|
| `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build` | Local testing, no domain — hits services directly on `:5173` / `:4000` / `:27017` |
| `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build` | Live, behind a real domain with HTTPS via Caddy on `:80`/`:443` |

**Never combine both** (`-f docker-compose.dev.yml -f docker-compose.prod.yml` together) —
they configure the same services differently and will conflict. The base file by
itself publishes no ports at all, so one of the two overlays is required either way.

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

| Type | Port | Source | Why |
|---|---|---|---|
| HTTP | 80 | 0.0.0.0/0 | Caddy's Let's Encrypt challenge + HTTPS redirect |
| HTTPS | 443 | 0.0.0.0/0 | The actual live traffic |
| SSH | 22 | your IP only | Keep this locked to your IP, not 0.0.0.0/0 |

Don't open 4000, 5173, 8080, or 27017 publicly — with the prod overlay, none of them
are even published to the host anymore (only `expose`d internally between
containers), so opening those ports in the security group wouldn't do anything useful
even if you did.

## 3. Point the Caddyfile at your real domain

Edit `infra/Caddyfile` and replace the placeholder domain in the "OPTION A" block:
```
your-domain.com {
```
with your actual domain, e.g. `chaipass.yourdomain.com {`.

**If DNS hasn't propagated yet** and you want to sanity-check the stack first, comment
out the Option A block and uncomment Option B (`:80 { ... }`) instead — that serves
plain HTTP on port 80 to anyone hitting your IP directly, no domain or TLS required.
Switch back to Option A once your domain resolves.

Also update `gateway.environment.CORS_ORIGIN` in `docker-compose.prod.yml` to your
real `https://your-domain.com`.

## 4. Bring it up with the production overlay

```bash
cd ~/chai/infra
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

## 5. Verify

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
curl -I https://chaipass.yourdomain.com     # or http://<ec2-ip> if using Option B
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs caddy
```
The `caddy` logs should show it obtaining a certificate on first start (Option A only).

## 6. Create your first account

The database starts empty — there is no seeded login. Register through the UI's
sign-up flow, or directly:
```bash
curl -X POST https://chaipass.yourdomain.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"role":"customer","name":"Test User","email":"you@example.com","phone":"9999999999","password":"a-strong-password"}'
```
Then log in with that email/password. `role` can be `customer` or `shop_owner` — admin
accounts aren't self-registrable by design (see `docs/ARCHITECTURE.md`); seed one
directly in MongoDB when you need it:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml exec mongo mongosh chaipass \
  --eval 'db.users.updateOne({email:"you@example.com"}, {$set:{role:"admin"}})'
```

## 7. Update `.env` for production values

- Generate strong random values for `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`,
  `SERVICE_JWT_SECRET`, `QR_JWT_SECRET`: `openssl rand -hex 32` for each.
- Set `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` / `RAZORPAY_WEBHOOK_SECRET` to your real
  (or test-mode) Razorpay dashboard values, and set the webhook URL there to
  `https://chaipass.yourdomain.com/api/payments/webhook`.

After changing `.env`, restart so the new values take effect:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

## 8. Ongoing operations

- **Logs:** `docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f <service>`
- **Restart one service:** `docker compose -f docker-compose.yml -f docker-compose.prod.yml restart gateway`
- **Redeploy after a code change:** `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build`
- **Certificate renewal:** automatic — Caddy handles this itself.
- **Mongo backups:** this setup runs a single-node Mongo for simplicity. Before real
  customer data goes in, either move to MongoDB Atlas (managed, backed up, and
  matches the `docs/ARCHITECTURE.md` production target) or set up a cron `mongodump`
  to S3 — ask me and I'll wire either one up.
