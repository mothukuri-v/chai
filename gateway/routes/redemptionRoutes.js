const express = require('express');
const rateLimit = require('express-rate-limit');
const QRCode = require('qrcode');
const { requireAuth, requireRole } = require('../middleware/auth');
const { callAs } = require('../services/businessServiceClient');

const router = express.Router();

// Redemption is the money/trust-critical path — throttle harder than the global limiter.
const redeemLimiter = rateLimit({ windowMs: 60 * 1000, max: 15, message: { code: 'RATE_LIMITED', message: 'Too many attempts, please slow down' } });

// Customer: today's status (already redeemed? credits left?)
router.get('/today', requireAuth, requireRole('customer'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get(`/redemption/today/${req.user.id}`);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

// Customer: mint today's single-use QR token (business service enforces all rules;
// gateway just renders whatever token it is handed as a scannable PNG/data-URI).
router.post('/token', requireAuth, requireRole('customer'), redeemLimiter, async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).post('/redemption/issue-token', { customerId: req.user.id });
    const qrImage = await QRCode.toDataURL(data.qrToken, { margin: 1, width: 320 });
    res.json({ qrImage, expiresAt: data.expiresAt });
  } catch (err) {
    next(err);
  }
});

// Shop owner: scans the customer's QR and submits it for validation.
router.post('/validate', requireAuth, requireRole('shop_owner'), redeemLimiter, async (req, res, next) => {
  try {
    const { qrToken } = req.body;
    if (!qrToken) return res.status(400).json({ code: 'VALIDATION_ERROR', message: 'qrToken is required' });

    const { data } = await callAs(req.user).post('/redemption/validate', {
      qrToken,
      shopOwnerId: req.user.id,
    });

    // Push a live "Tea Approved" event to the customer's open session, if any.
    if (data.customerId) {
      req.app.get('io').to(`user:${data.customerId}`).emit('tea:approved', {
        shopName: data.shopName,
        redeemedAt: data.redeemedAt,
      });
    }

    res.json(data);
  } catch (err) {
    next(err);
  }
});

router.get('/history', requireAuth, requireRole('customer'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get(`/redemption/customer/${req.user.id}/history`);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
