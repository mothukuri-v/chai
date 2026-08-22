const logger = require('../services/logger');

// Business-service errors arrive as { code, message } — pass them through untouched so
// the frontend can render a precise state (e.g. ALREADY_REDEEMED_TODAY, QR_EXPIRED).
function errorHandler(err, req, res, next) { // eslint-disable-line no-unused-vars
  const status = err.status || err.response?.status || 500;
  const code = err.response?.data?.code || err.code || 'INTERNAL_ERROR';
  const message = err.response?.data?.message || err.message || 'Something went wrong';

  if (status >= 500) logger.error(`${req.method} ${req.originalUrl} -> ${status} ${code}`, { stack: err.stack });
  else logger.warn(`${req.method} ${req.originalUrl} -> ${status} ${code}`);

  res.status(status).json({ code, message });
}

module.exports = { errorHandler };
