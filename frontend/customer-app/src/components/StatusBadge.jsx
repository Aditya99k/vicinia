import { statusLabel, statusTone, TONE_CLASS } from '../utils/status';

export default function StatusBadge({ status, label }) {
  const tone = statusTone(status);
  return (
    <span className={`badge ${TONE_CLASS[tone]}`}>
      <span className="badge-dot" />
      {label || statusLabel(status)}
    </span>
  );
}
