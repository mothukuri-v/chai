const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');
const { callAs } = require('../services/businessServiceClient');

const router = express.Router();
router.use(requireAuth, requireRole('admin'));

router.get('/overview', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get('/admin/overview');
    res.json(data); // totalUsers, activeSubscriptions, teaShops, todaysRedemptions, revenue, failedPayments
  } catch (err) { next(err); }
});

router.get('/users', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get('/admin/users', { params: req.query });
    res.json(data);
  } catch (err) { next(err); }
});

router.patch('/users/:id/status', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).post(`/admin/users/${req.params.id}/status`, req.body);
    res.json(data);
  } catch (err) { next(err); }
});

router.get('/shops/pending', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get('/admin/shops/pending');
    res.json(data);
  } catch (err) { next(err); }
});

router.post('/shops/:id/approve', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).post(`/admin/shops/${req.params.id}/approve`, {});
    res.json(data);
  } catch (err) { next(err); }
});

router.post('/shops/:id/reject', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).post(`/admin/shops/${req.params.id}/reject`, req.body);
    res.json(data);
  } catch (err) { next(err); }
});

router.post('/payments/:id/refund', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).post(`/admin/payments/${req.params.id}/refund`, req.body);
    res.json(data);
  } catch (err) { next(err); }
});

router.get('/redemptions/live', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get('/admin/redemptions/live');
    res.json(data);
  } catch (err) { next(err); }
});

router.get('/audit-logs', async (req, res, next) => {
  try {
    const { data } = await callAs(req.user).get('/admin/audit-logs', { params: req.query });
    res.json(data);
  } catch (err) { next(err); }
});

module.exports = router;
