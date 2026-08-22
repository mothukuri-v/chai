const jwt = require('jsonwebtoken');

/** Verifies the customer-facing access token and attaches req.user = { id, role }. */
function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ code: 'NO_TOKEN', message: 'Authentication required' });

  try {
    const payload = jwt.verify(token, process.env.JWT_ACCESS_SECRET);
    req.user = { id: payload.sub, role: payload.role, name: payload.name };
    next();
  } catch (err) {
    return res.status(401).json({ code: 'INVALID_TOKEN', message: 'Session expired or invalid, please log in again' });
  }
}

/** Role-based access control — usage: requireRole('admin') or requireRole('admin', 'shop_owner') */
function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({ code: 'FORBIDDEN', message: 'You do not have access to this resource' });
    }
    next();
  };
}

module.exports = { requireAuth, requireRole };
