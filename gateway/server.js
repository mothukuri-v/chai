require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const morgan = require('morgan');
const cookieParser = require('cookie-parser');
const mongoose = require('mongoose');
const rateLimit = require('express-rate-limit');
const http = require('http');
const { Server } = require('socket.io');

const authRoutes = require('./routes/authRoutes');
const subscriptionRoutes = require('./routes/subscriptionRoutes');
const redemptionRoutes = require('./routes/redemptionRoutes');
const shopRoutes = require('./routes/shopRoutes');
const paymentRoutes = require('./routes/paymentRoutes');
const adminRoutes = require('./routes/adminRoutes');
const { errorHandler } = require('./middleware/errorHandler');
const logger = require('./services/logger');

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: (process.env.CORS_ORIGIN || 'http://localhost:5173').split(','), credentials: true } });
app.set('io', io); // used to push live "tea approved" events to customers

app.use(helmet());
app.use(cors({
  origin: (process.env.CORS_ORIGIN || 'http://localhost:5173').split(','),
  credentials: true,
}));
app.use(cookieParser());
app.use(morgan('combined', { stream: { write: (msg) => logger.info(msg.trim()) } }));

// Razorpay webhook needs the RAW body for signature verification, so it is
// mounted before the JSON parser with its own raw parser (see paymentRoutes).
app.use('/api/payments/webhook', express.raw({ type: '*/*' }));
app.use(express.json({ limit: '1mb' }));

// Global rate limit; redemption + login get stricter limits inside their routers.
app.use(rateLimit({ windowMs: 60 * 1000, max: 120, standardHeaders: true, legacyHeaders: false }));

app.get('/health', (req, res) => res.json({ status: 'ok', service: 'gateway' }));

app.use('/api/auth', authRoutes);
app.use('/api/subscriptions', subscriptionRoutes);
app.use('/api/redemption', redemptionRoutes);
app.use('/api/shops', shopRoutes);
app.use('/api/payments', paymentRoutes);
app.use('/api/admin', adminRoutes);

app.use((req, res) => res.status(404).json({ code: 'NOT_FOUND', message: 'Route not found' }));
app.use(errorHandler);

io.on('connection', (socket) => {
  socket.on('join', (userId) => socket.join(`user:${userId}`));
});

const PORT = process.env.PORT || 4000;

mongoose
  .connect(process.env.MONGO_URI)
  .then(() => {
    logger.info('Connected to MongoDB');
    server.listen(PORT, () => logger.info(`Gateway listening on :${PORT}`));
  })
  .catch((err) => {
    logger.error('Mongo connection failed', err);
    process.exit(1);
  });

module.exports = { app, server };
