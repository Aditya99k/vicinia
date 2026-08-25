import { useCallback, useRef, useState } from 'react';
import ActionDialog from '../components/ActionDialog';

/**
 * Promise-based replacement for window.confirm/window.prompt, styled like
 * the rest of the app instead of an unstyled native dialog. Usage:
 *
 *   const { confirm, promptText, dialog } = useActionDialog();
 *   ...
 *   if (!(await confirm('Cancel this order?'))) return;
 *   const reason = await promptText('Reason for rejecting?');
 *   if (reason === null) return; // cancelled
 *   ...
 *   return <>{dialog}{...rest of page}</>;
 */
export function useActionDialog() {
  const [state, setState] = useState(null);
  const resolverRef = useRef(null);

  const confirm = useCallback((message, opts = {}) => {
    return new Promise((resolve) => {
      resolverRef.current = resolve;
      setState({ mode: 'confirm', message, ...opts });
    });
  }, []);

  const promptText = useCallback((message, opts = {}) => {
    return new Promise((resolve) => {
      resolverRef.current = resolve;
      setState({ mode: 'prompt', message, ...opts });
    });
  }, []);

  function handleCancel() {
    resolverRef.current?.(state.mode === 'prompt' ? null : false);
    setState(null);
  }

  function handleConfirm(value) {
    resolverRef.current?.(value);
    setState(null);
  }

  const dialog = state ? (
    <ActionDialog {...state} onCancel={handleCancel} onConfirm={handleConfirm} />
  ) : null;

  return { confirm, promptText, dialog };
}
