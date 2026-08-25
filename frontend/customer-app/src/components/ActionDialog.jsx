import { useState } from 'react';
import { AlertIcon } from './Icons';

/** Renders whichever dialog useActionDialog's `dialog` value currently holds — confirm (yes/no) or prompt (free-text reason), styled like every other modal in the app instead of a native window.confirm/prompt. */
export default function ActionDialog({ mode, title, message, confirmLabel, danger, placeholder, defaultValue, onCancel, onConfirm }) {
  const [value, setValue] = useState(defaultValue || '');

  function handleSubmit(e) {
    e.preventDefault();
    onConfirm(mode === 'prompt' ? value : true);
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal-sheet action-dialog" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          {danger && (
            <div className="action-dialog-icon danger"><AlertIcon style={{ width: 20, height: 20 }} /></div>
          )}
          <h3 className="action-dialog-title">{title}</h3>
          {message && <p className="action-dialog-message">{message}</p>}

          {mode === 'prompt' && (
            <textarea
              className="action-dialog-input"
              rows={2}
              placeholder={placeholder || 'Optional…'}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              autoFocus
            />
          )}

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
            <button type="submit" className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} autoFocus={mode === 'confirm'}>
              {confirmLabel || (mode === 'prompt' ? 'Submit' : 'Confirm')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
