import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const DASHBOARD_BY_ROLE = { customer: '/customer', shop_owner: '/shop', admin: '/admin' };

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const user = await login(email, password);
      navigate(DASHBOARD_BY_ROLE[user.role] || '/customer');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    }
  }

  return (
    <div style={{ maxWidth: 380, margin: '80px auto', padding: '0 20px' }}>
      <h1 style={{ fontSize: 28 }}>Welcome back</h1>
      <p style={{ color: 'var(--ink-muted)', fontSize: 14 }}>Sign in to get today's tea.</p>
      <form onSubmit={handleSubmit} className="card" style={{ marginTop: 20 }}>
        <label style={{ fontSize: 13, fontWeight: 700 }}>Email</label>
        <input
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          style={{ width: '100%', padding: 10, margin: '6px 0 16px', borderRadius: 10, border: '1px solid var(--line)' }}
        />
        <label style={{ fontSize: 13, fontWeight: 700 }}>Password</label>
        <input
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          style={{ width: '100%', padding: 10, margin: '6px 0 16px', borderRadius: 10, border: '1px solid var(--line)' }}
        />
        {error && <p style={{ color: '#a13a2b', fontSize: 13 }}>{error}</p>}
        <button className="btn btn-primary" type="submit" style={{ width: '100%' }}>Sign in</button>
      </form>
    </div>
  );
}
