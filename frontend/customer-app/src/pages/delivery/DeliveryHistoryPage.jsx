import { useEffect, useState } from 'react';
import { getHistory } from '../../utils/deliveryHistory';
import StatusBadge from '../../components/StatusBadge';
import { EmptyBoxIllustration } from '../../components/Illustrations';
import { formatDateTime } from '../../utils/format';

export default function DeliveryHistoryPage() {
  const [history, setHistory] = useState([]);

  useEffect(() => setHistory(getHistory()), []);

  return (
    <div>
      <h1 style={{ fontSize: 22, marginBottom: 4 }}>Task history</h1>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 18 }}>Tasks you've worked on from this device.</p>

      {history.length === 0 ? (
        <div className="empty-state">
          <EmptyBoxIllustration />
          <h3>No history yet</h3>
          <p>Tasks you accept, pick up, and deliver will be listed here.</p>
        </div>
      ) : (
        <div className="order-list">
          {history.map((h) => (
            <div className="order-row card" key={h.orderId + h.at}>
              <div>
                <div className="order-row-id">Order #{h.orderId.slice(0, 8)}</div>
                <div className="muted">{formatDateTime(h.at)}</div>
              </div>
              <StatusBadge status={h.status} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
