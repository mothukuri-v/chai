# API Contract (Gateway — public surface)

All routes are prefixed `/api`. Auth via `Authorization: Bearer <accessToken>` unless noted.

## Auth
| Method | Path | Role | Notes |
|---|---|---|---|
| POST | /auth/register | public | `{ role: customer\|shop_owner, name, email, phone, password }` |
| POST | /auth/login | public | rate-limited 10/15min |
| POST | /auth/refresh | public (refresh cookie) | rotates access token |
| POST | /auth/logout | any | clears refresh cookie |

## Subscriptions
| Method | Path | Role |
|---|---|---|
| GET | /subscriptions/me | customer |
| POST | /subscriptions/checkout | customer — returns Razorpay order for checkout |
| GET | /subscriptions/history | customer |

## Redemption
| Method | Path | Role |
|---|---|---|
| GET | /redemption/today | customer |
| POST | /redemption/token | customer — mints 90s single-use QR |
| POST | /redemption/validate | shop_owner — `{ qrToken }`, the critical-path endpoint |
| GET | /redemption/history | customer |

## Shops
| Method | Path | Role |
|---|---|---|
| GET | /shops/nearby?lat&lng&radiusKm | any authenticated |
| POST | /shops | shop_owner |
| GET | /shops/mine | shop_owner |
| GET | /shops/mine/analytics | shop_owner |
| GET | /shops/mine/redemptions/today | shop_owner |

## Payments
| Method | Path | Role |
|---|---|---|
| POST | /payments/verify | customer — client-side checkout confirmation |
| POST | /payments/webhook | Razorpay (HMAC-signed, no user auth) |
| GET | /payments/history | customer |

## Admin
| Method | Path | Role |
|---|---|---|
| GET | /admin/overview | admin |
| GET | /admin/users | admin |
| PATCH | /admin/users/:id/status | admin |
| GET | /admin/shops/pending | admin |
| POST | /admin/shops/:id/approve | admin |
| POST | /admin/shops/:id/reject | admin |
| POST | /admin/payments/:id/refund | admin |
| GET | /admin/redemptions/live | admin |
| GET | /admin/audit-logs | admin |

## Error shape
```json
{ "code": "ALREADY_REDEEMED_TODAY", "message": "You've already redeemed your tea for today. Come back tomorrow!" }
```
Codes used on the redemption path: `SUBSCRIPTION_INACTIVE`, `ALREADY_REDEEMED_TODAY`, `QR_EXPIRED`,
`QR_INVALID`, `QR_ALREADY_USED`, `SHOP_NOT_VERIFIED`. See `docs/ARCHITECTURE.md` for how each is enforced.
