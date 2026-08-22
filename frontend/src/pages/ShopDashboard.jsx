import React, { useEffect, useState } from 'react';
import api from '../api/client';

export default function ShopDashboard() {
  const [todayCount, setTodayCount] = useState(null);
  const [qrToken, setQrToken] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => {
    api.get('/shops/mine/redemptions/today').then((r) => setTodayCount(r.data.count)).catch(() => {});
  }, []);

  async function validate(e) {
    e.preventDefault();
    setResult(null);
    try {
      const { data } = await api.post('/redemption/validate', { qrToken });
      setResult({ ok: true, message: `Approved — ${data.teaCreditsRemaining} credits left` });
      setTodayCount((c) => (c ?? 0) + 1);
    } catch (err) {
      setResult({ ok: false, message: err.response?.data?.message || 'Validation failed' });
    }
    setQrToken('');
  }

  return (
    <div style={{ maxWidth: 560, margin: '40px auto', padding: '0 20px' }}>
      <h1>Scan customer QR</h1>
      <p style={{ color: 'var(--ink-muted)', fontSize: 14 }}>Today's redemptions: {todayCount ?? '—'}</p>

      <form className="card" onSubmit={validate}>
        <label style={{ fontSize: 13, fontWeight: 700 }}>Scanned QR token</label>
        <input
          value={qrToken}
          onChange={(e) => setQrToken(e.target.value)}
          required
          style={{ width: '100%', padding: 10, margin: '6px 0 16px', borderRadius: 10, border: '1px solid var(--line)' }}
        />
        <button className="btn btn-primary" type="submit">Validate</button>
      </form>

      {result && (
        <p style={{ marginTop: 16, fontWeight: 700, color: result.ok ? '#3f6b3f' : '#a13a2b' }}>{result.message}</p>
      )}
    </div>
  );
}
