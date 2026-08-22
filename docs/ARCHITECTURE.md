# Architecture

## 5-Tier flow

1. **React.js Frontend** — role-aware SPA. Stores only the JWT access token (memory + refresh
   cookie), never business state. Talks exclusively to the Node gateway.
2. **Node.js + Express API Gateway (BFF)**
   - Terminates TLS, applies `helmet`, CORS allow-list, rate limiting (`express-rate-limit`,
     stricter on `/redeem` and `/auth/login`).
   - Issues/verifies JWTs (`RS256`), attaches `req.user` from token, enforces RBAC middleware
     per route (`customer`, `shop_owner`, `admin`).
   - Generates QR payloads (shop QR = static per shop; redemption uses a **short-lived signed
     token**, see below) via `qrcode` npm package, returns as data-URI/PNG.
   - Proxies all business-rule decisions to Spring Boot over an internal network with a
     service-to-service JWT (`aud: business-service`), never trusting the client for anything
     that affects money or redemption state.
   - Owns the Razorpay integration: order creation, checkout handoff, **webhook signature
     verification** (HMAC SHA256 with `X-Razorpay-Signature`), then forwards a verified payment
     event to Spring Boot for reconciliation — the gateway itself never marks a subscription
     "paid" from client-supplied data.
3. **Java + Spring Boot Business Services**
   - `SubscriptionService` — creates/renews subscriptions, computes `daysRemaining`,
     `teaCreditsRemaining`, expiry.
   - `QrService` — issues and validates the redemption QR token (JWT, 90s TTL, single-use
     `jti`, bound to `customerId`); issues static shop-identity QR (bound to `shopId`, rotated
     on demand by the owner).
   - `RedemptionService` — the critical path (see below). Runs inside a MongoDB **transaction**
     with a **unique compound index** `(customerId, redemptionDate)` so a duplicate can never
     be written even under concurrent requests.
   - `PaymentService` — reconciles Razorpay events idempotently keyed on `razorpay_payment_id`
     (unique index) to make duplicate webhook delivery a no-op.
   - Cross-cutting: Bean Validation on every DTO, `@ControllerAdvice` for typed error codes,
     structured audit logging to the `audit_logs` collection for every state-changing call.
4. **Data Access Layer** — Spring Data MongoDB repositories, one per aggregate, with explicit
   indices (see `docs/DATA_MODEL.md`) and `@Transactional` where multi-document consistency
   matters (redemption + subscription counter decrement).
5. **MongoDB** — replica set (even single-node in dev, 3-node in prod) so multi-document
   transactions are available.

## The redemption critical path

```
Customer taps "Get Today's Tea"
   → Node: GET /api/redemption/token  (JWT auth)
   → Java:  QrService.issueRedemptionToken(customerId)
            - checks subscription is ACTIVE and not expired         [fail: SUBSCRIPTION_INACTIVE]
            - checks no redemption already exists for today          [fail: ALREADY_REDEEMED_TODAY]
            - mints a 90s single-use JWT {customerId, jti, exp}
   ← QR rendered on customer's phone

Shop owner scans QR with the in-app scanner
   → Node: POST /api/redemption/validate  { qrToken, shopId }
   → Java:  RedemptionService.validate(qrToken, shopId)
            1. verify JWT signature + expiry                          [fail: QR_EXPIRED / QR_INVALID]
            2. verify jti not already consumed (Mongo unique index)   [fail: QR_ALREADY_USED]
            3. re-check subscription still ACTIVE                     [fail: SUBSCRIPTION_INACTIVE]
            4. re-check no redemption row for (customerId, today)     [fail: ALREADY_REDEEMED_TODAY]
            5. verify shop is VERIFIED and ACTIVE                     [fail: SHOP_NOT_VERIFIED]
            6. START MONGO TRANSACTION
               - insert redemption { customerId, shopId, date, jti }  (unique index enforces atomicity)
               - decrement subscription.teaCreditsRemaining
               - append audit_log entry
               COMMIT
            7. return SUCCESS + redemption receipt
   ← Node relays SUCCESS/FAILURE to shop owner's screen (and pushes a
     notification to the customer via SSE/socket if connected)
```

Because step 6 relies on a **unique index on `(customerId, redemptionDate)`**, even two
simultaneous scans (e.g. a retried request) cannot both succeed — the second write throws a
duplicate-key error which the service maps to `ALREADY_REDEEMED_TODAY`. This is the actual
mechanism that makes "one tea per day" airtight, not just an application-level `if` check.

## Defence against the required threat list

| Threat                        | Mitigation |
|---|---|
| Duplicate redemption            | Unique Mongo index `(customerId, redemptionDate)` + single-use `jti` index, both enforced inside a transaction |
| Expired subscription            | Re-validated at token issuance **and** at scan time (not trusted from the QR payload) |
| Fake / replayed QR               | QR is a signed JWT (RS256) minted by Spring Boot only, 90s TTL, single-use `jti`; gateway never generates it client-side |
| Unauthorized shop scanning        | Shop must hold a valid `shop_owner` JWT **and** the shop document must be `VERIFIED`; scanning endpoint checks `shopId` from token, not from request body |
| Unauthorized user actions          | RBAC middleware in Node gateway on every route + re-checked by role claim inside Spring Boot service calls (defence in depth) |
| Duplicate payment processing        | Razorpay `payment_id` unique index; webhook handler is idempotent; signature verified before any DB write |

## Deployment (Docker + AWS)

- Each tier ships as its own Docker image (`frontend`, `gateway`, `business-service`).
- `infra/docker-compose.yml` runs the full stack locally, `infra/aws/` notes the production
  target: React build → S3 + CloudFront; Node gateway + Spring Boot → ECS Fargate services
  behind an ALB (path-based routing `/api/*` → gateway only; Spring Boot has **no public
  ingress**, reachable only from the gateway's security group); MongoDB → Atlas (or
  DocumentDB) with a private VPC peering link; secrets in AWS Secrets Manager, pulled at
  container start.

## CI/CD

`infra/Jenkinsfile` — checkout → parallel build (`npm ci && npm run build` for
frontend/gateway, `mvn -B verify` for business-service) → SonarQube analysis
(`mvn sonar:sonar` / `sonar-scanner`) with a quality-gate wait step → Docker build & push to
ECR → deploy stage (`aws ecs update-service --force-new-deployment`), gated on the SonarQube
quality gate and test pass.
