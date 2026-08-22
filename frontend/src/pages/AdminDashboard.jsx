import React, { useEffect, useState } from 'react';
import api from '../api/client';

export default function AdminDashboard() {
  const [overview, setOverview] = useState(null);

  useEffect(() => {
    api.get('/admin/overview').then((r) => setOverview(r.data)).catch(() => {});
  }, []);

  if (!overview) return <div style={{ padding: 40 }}>Loading overview…</div>;

  const stats = [
    ['Active subscriptions', overview.activeSubscriptions],
    ['Verified shops', overview.verifiedShops],
    ["Today's redemptions", overview.todaysRedemptions],
    ['Revenue (INR)', overview.revenueInr],
    ['Failed payments', overview.failedPayments],
  ];

  return (
    <div style={{ maxWidth: 900, margin: '40px auto', padding: '0 20px' }}>
      <h1>Platform overview</h1>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 16 }}>
        {stats.map(([label, value]) => (
          <div className="card" key={label}>
            <p style={{ fontSize: 12, color: 'var(--ink-muted)', margin: 0 }}>{label}</p>
            <p style={{ fontSize: 26, fontFamily: 'Fraunces', margin: '4px 0 0' }}>{value}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
