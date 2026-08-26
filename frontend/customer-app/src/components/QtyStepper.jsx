import { MinusIcon, PlusIcon } from './Icons';

/** Blinkit-style "− N +" control that replaces an Add button once a listing is already in the cart — same footprint as the .btn.btn-sm it stands in for, so swapping between the two doesn't jump the layout. */
export default function QtyStepper({ quantity, busy, maxReached, onIncrement, onDecrement }) {
  function stop(e, fn) {
    e.preventDefault();
    e.stopPropagation();
    fn();
  }

  return (
    <div className="qty-stepper-btn">
      <button type="button" onClick={(e) => stop(e, onDecrement)} disabled={busy} aria-label="Remove one">
        <MinusIcon style={{ width: 12, height: 12 }} />
      </button>
      <span>{busy ? <span className="spinner" /> : quantity}</span>
      <button type="button" onClick={(e) => stop(e, onIncrement)} disabled={busy || maxReached} aria-label="Add one more">
        <PlusIcon style={{ width: 12, height: 12 }} />
      </button>
    </div>
  );
}
