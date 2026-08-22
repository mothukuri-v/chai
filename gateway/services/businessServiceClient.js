const axios = require('axios');
const jwt = require('jsonwebtoken');

/**
 * All money- and redemption-critical logic lives in Spring Boot. This client signs a
 * short-lived internal service token per request so Spring Boot can trust the caller
 * identity/role without re-implementing session auth.
 */
const client = axios.create({
  baseURL: process.env.BUSINESS_SERVICE_URL,
  timeout: 5000,
});

function serviceToken(user) {
  return jwt.sign(
    { sub: user.id, role: user.role, aud: 'business-service' },
    process.env.SERVICE_JWT_SECRET,
    { expiresIn: '30s' }
  );
}

function callAs(user) {
  const token = serviceToken(user);
  return {
    get: (url, config = {}) => client.get(url, { ...config, headers: { ...config.headers, 'X-Service-Token': token } }),
    post: (url, data, config = {}) => client.post(url, data, { ...config, headers: { ...config.headers, 'X-Service-Token': token } }),
  };
}

module.exports = { callAs };
