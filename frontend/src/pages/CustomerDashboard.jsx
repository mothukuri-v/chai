import React, { useEffect, useState } from 'react';
import api from '../api/client';

export default function CustomerDashboard() {
  const [subscription, setSubscription] = useState(null);
  const [today, setToday] = useState(null);
  const [qrImage, setQrImage] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/subscriptions/me').then((r) => setSubscription(r.data)).catch(() => {});
    api.get('/redemption/today').then((r) => setToday(r.data)).catch(() => {});
  }, []);

  async function getTeaQr() {
    setError('');
    try {
      const { data } = await api.post('/redemption/token');
      setQrImage(data.qrImage);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not generate QR');
    }
  }

  return (
    <div style={{ maxWidth: 720, margin: '40px auto', padding: '0 20px' }}>
      <h1>Your subscription</h1>

      {subscription ? (
        <div className="card" style={{ background: 'var(--tea-900)', color: 'var(--cream)' }}>
          <p style={{ margin: 0, fontSize: 13, opacity: 0.8 }}>Tea credits remaining</p>
          <p style={{ fontSize: 32, margin: '4px 0', fontFamily: 'Fraunces' }}>{subscription.teaCreditsRemaining}</p>
          <p style={{ fontSize: 12, opacity: 0.7 }}>Valid until {new Date(subscription.endDate).toLocaleDateString()}</p>
        </div>
      ) : (
        <p style={{ color: 'var(--ink-muted)' }}>No active subscription — checkout to get started.</p>
      )}

      <div className="card" style={{ marginTop: 20, textAlign: 'center' }}>
        {today?.redeemedToday ? (
          <p>You've already had today's tea. Come back tomorrow!</p>
        ) : qrImage ? (
          <>
            <img src={qrImage} alt="Redemption QR code" style={{ width: 220 }} />
            <p style={{ fontSize: 12, color: 'var(--ink-muted)' }}>Show this to the shop — expires in 90s</p>
          </>
        ) : (
          <button className="btn btn-primary" onClick={getTeaQr}>Get today's tea</button>
        )}
        {error && <p style={{ color: '#a13a2b', fontSize: 13 }}>{error}</p>}
      </div>
    </div>
  );
}
