const express = require('express');
const crypto = require('crypto');
const { requireAuth, requireRole } = require('../middleware/auth');
const { callAs } = require('../services/businessServiceClient');
const logger = require('../services/logger');

const router = express.Router();

// Client confirms checkout success (belt) — the webhook below is the source of truth (braces).
router.post('/verify', requireAuth, requireRole('customer'), async (req, res, next) => {
  try {
    const { razorpay_order_id, razorpay_payment_id, razorpay_signature } = req.body;
    const expected = crypto
      .createHmac('sha256', process.env.RAZORPAY_KEY_SECRET)
      .update(`${razorpay_order_id}|${razorpay_payment_id}`)
      .digest('hex');

    if (expected !== razorpay_signature) {
      return res.status(400).json({ code: 'PAYMENT_SIGNATURE_INVALID', message: 'Payment could not be verified' });
    }

    const { data } = await callAs(req.user).post('/payments/confirm', {
      razorpayOrderId: razorpay_order_id,
      razorpayPaymentId: razorpay_payment_id,
      customerId: req.user.id,
    });
    res.json(data);
  } catch (err) {
    next(err);
  }
});

// Razorpay server-to-server webhook — source of truth for payment state.
// Mounted with express.raw() in server.js so the raw bytes are available for HMAC verification.
router.post('/webhook', async (req, res) => {
  try {
    const signature = req.headers['x-razorpay-signature'];
    const expected = crypto
      .createHmac('sha256', process.env.RAZORPAY_WEBHOOK_SECRET)
      .update(req.body) // raw Buffer
      .digest('hex');

    if (signature !== expected) {
      logger.warn('Rejected webhook: bad signature');
      return res.status(400).json({ code: 'INVALID_SIGNATURE' });
    }

    const event = JSON.parse(req.body.toString('utf8'));

    // Idempotency belongs to the business service (unique index on razorpayPaymentId) —
    // the gateway just relays every verified event; duplicates are a safe no-op downstream.
    await callAs({ id: 'system', role: 'admin' }).post('/payments/webhook-event', event);

    res.json({ received: true });
  } catch (err) {
    logger.error('Webhook processing failed', err);
    res.status(500).json({ code: 'WEBHOOK_ERROR' });
  }
});

router.get('/history', requireAuth, requireRole('customer'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get(`/payments/customer/${req.user.id}`);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
