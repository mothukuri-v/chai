# MongoDB Data Model

## `users`
```json
{
  "_id": "ObjectId",
  "role": "customer | shop_owner | admin",
  "name": "string",
  "email": "string (unique index)",
  "phone": "string (unique index)",
  "passwordHash": "string",
  "status": "ACTIVE | SUSPENDED",
  "createdAt": "date"
}
```

## `shops`
```json
{
  "_id": "ObjectId",
  "ownerId": "ObjectId -> users",
  "name": "string",
  "address": "string",
  "location": { "type": "Point", "coordinates": [77.57, 12.97] },   // 2dsphere index
  "status": "PENDING | VERIFIED | REJECTED | SUSPENDED",
  "qrIdentity": "string (static shop token, rotatable)",
  "createdAt": "date"
}
```
Index: `{ location: "2dsphere" }` for "nearby shops"; `{ ownerId: 1 }`.

## `subscriptions`
```json
{
  "_id": "ObjectId",
  "customerId": "ObjectId -> users",
  "plan": "MONTHLY_30",
  "amount": 500,
  "startDate": "date",
  "endDate": "date",
  "teaCreditsTotal": 30,
  "teaCreditsRemaining": 21,
  "status": "ACTIVE | EXPIRED | CANCELLED",
  "paymentId": "ObjectId -> payments"
}
```
Index: `{ customerId: 1, status: 1 }`.

## `redemptions`  ← enforces the core business rule
```json
{
  "_id": "ObjectId",
  "customerId": "ObjectId -> users",
  "shopId": "ObjectId -> shops",
  "subscriptionId": "ObjectId -> subscriptions",
  "redemptionDate": "string (YYYY-MM-DD, customer's local day)",
  "jti": "string (QR token id, unique)",
  "redeemedAt": "date"
}
```
**Unique compound index:** `{ customerId: 1, redemptionDate: 1 }` — a duplicate insert throws
`E11000`, which the service layer maps to `ALREADY_REDEEMED_TODAY`.
**Unique index:** `{ jti: 1 }` — prevents QR replay even within the same day.

## `payments`
```json
{
  "_id": "ObjectId",
  "customerId": "ObjectId -> users",
  "razorpayOrderId": "string",
  "razorpayPaymentId": "string (unique index — idempotency key)",
  "amount": 500,
  "currency": "INR",
  "status": "CREATED | PAID | FAILED | REFUNDED",
  "createdAt": "date"
}
```

## `audit_logs`
```json
{
  "_id": "ObjectId",
  "actorId": "ObjectId",
  "actorRole": "string",
  "action": "REDEMPTION_SUCCESS | SHOP_VERIFIED | PAYMENT_REFUNDED | ...",
  "targetId": "ObjectId",
  "metadata": {},
  "timestamp": "date"
}
```
