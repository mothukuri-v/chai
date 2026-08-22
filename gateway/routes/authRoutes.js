const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
const { z } = require('zod');
const User = require('../models/User');

const router = express.Router();

const loginLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 10, message: { code: 'TOO_MANY_ATTEMPTS', message: 'Too many login attempts, try again later' } });

const registerSchema = z.object({
  role: z.enum(['customer', 'shop_owner']), // admins are seeded, never self-registered
  name: z.string().min(2),
  email: z.string().email(),
  phone: z.string().min(10).max(15),
  password: z.string().min(8),
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

function issueTokens(user) {
  const accessToken = jwt.sign(
    { sub: user._id.toString(), role: user.role, name: user.name },
    process.env.JWT_ACCESS_SECRET,
    { expiresIn: process.env.JWT_ACCESS_TTL || '15m' }
  );
  const refreshToken = jwt.sign({ sub: user._id.toString() }, process.env.JWT_REFRESH_SECRET, {
    expiresIn: process.env.JWT_REFRESH_TTL || '7d',
  });
  return { accessToken, refreshToken };
}

router.post('/register', async (req, res, next) => {
  try {
    const input = registerSchema.parse(req.body);
    const existing = await User.findOne({ $or: [{ email: input.email }, { phone: input.phone }] });
    if (existing) return res.status(409).json({ code: 'USER_EXISTS', message: 'An account with this email or phone already exists' });

    const passwordHash = await bcrypt.hash(input.password, 12);
    const user = await User.create({ ...input, passwordHash });
    const tokens = issueTokens(user);

    res
      .cookie('refreshToken', tokens.refreshToken, { httpOnly: true, sameSite: 'lax', maxAge: 7 * 24 * 3600 * 1000 })
      .status(201)
      .json({ accessToken: tokens.accessToken, user: { id: user._id, name: user.name, role: user.role, email: user.email } });
  } catch (err) {
    if (err.name === 'ZodError') return res.status(400).json({ code: 'VALIDATION_ERROR', message: err.errors[0].message });
    next(err);
  }
});

router.post('/login', loginLimiter, async (req, res, next) => {
  try {
    const input = loginSchema.parse(req.body);
    const user = await User.findOne({ email: input.email });
    if (!user || user.status !== 'ACTIVE') return res.status(401).json({ code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' });

    const valid = await bcrypt.compare(input.password, user.passwordHash);
    if (!valid) return res.status(401).json({ code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' });

    const tokens = issueTokens(user);
    res
      .cookie('refreshToken', tokens.refreshToken, { httpOnly: true, sameSite: 'lax', maxAge: 7 * 24 * 3600 * 1000 })
      .json({ accessToken: tokens.accessToken, user: { id: user._id, name: user.name, role: user.role, email: user.email } });
  } catch (err) {
    if (err.name === 'ZodError') return res.status(400).json({ code: 'VALIDATION_ERROR', message: err.errors[0].message });
    next(err);
  }
});

router.post('/refresh', async (req, res) => {
  const token = req.cookies?.refreshToken;
  if (!token) return res.status(401).json({ code: 'NO_REFRESH_TOKEN', message: 'Please log in again' });
  try {
    const payload = jwt.verify(token, process.env.JWT_REFRESH_SECRET);
    const user = await User.findById(payload.sub);
    if (!user || user.status !== 'ACTIVE') return res.status(401).json({ code: 'INVALID_TOKEN', message: 'Please log in again' });
    const tokens = issueTokens(user);
    res.cookie('refreshToken', tokens.refreshToken, { httpOnly: true, sameSite: 'lax', maxAge: 7 * 24 * 3600 * 1000 });
    res.json({ accessToken: tokens.accessToken });
  } catch {
    res.status(401).json({ code: 'INVALID_TOKEN', message: 'Please log in again' });
  }
});

router.post('/logout', (req, res) => {
  res.clearCookie('refreshToken').json({ message: 'Logged out' });
});

module.exports = router;
