const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');
const { callAs } = require('../services/businessServiceClient');

const router = express.Router();

// Current subscription status: days left, tea credits left, expiry
router.get('/me', requireAuth, requireRole('customer'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get(`/subscriptions/customer/${req.user.id}`);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

// Kick off a subscription purchase: gateway creates a Razorpay order, business service
// records a PENDING subscription/payment pair so both sides agree before money moves.
router.post('/checkout', requireAuth, requireRole('customer'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).post('/subscriptions/checkout', {
      customerId: req.user.id,
      plan: 'MONTHLY_30',
    });
    // data = { subscriptionId, razorpayOrderId, amount, currency, keyId }
    res.json(data);
  } catch (err) {
    next(err);
  }
});

router.get('/history', requireAuth, requireRole('customer'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get(`/subscriptions/customer/${req.user.id}/history`);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
