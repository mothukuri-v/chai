const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');
const { callAs } = require('../services/businessServiceClient');

const router = express.Router();

// Public-ish: nearby verified shops for the customer map view (still requires login)
router.get('/nearby', requireAuth, async (req, res, next) => {
  try {
    const { lat, lng, radiusKm = 5 } = req.query;
    const { data } = await callAs(req.user).get('/shops/nearby', { params: { lat, lng, radiusKm } });
    res.json(data);
  } catch (err) {
    next(err);
  }
});

router.post('/', requireAuth, requireRole('shop_owner'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).post('/shops', { ...req.body, ownerId: req.user.id });
    res.status(201).json(data);
  } catch (err) {
    next(err);
  }
});

router.get('/mine', requireAuth, requireRole('shop_owner'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get(`/shops/owner/${req.user.id}`);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

router.get('/mine/analytics', requireAuth, requireRole('shop_owner'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get(`/shops/owner/${req.user.id}/analytics`);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

router.get('/mine/redemptions/today', requireAuth, requireRole('shop_owner'), async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get(`/shops/owner/${req.user.id}/redemptions/today`);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
