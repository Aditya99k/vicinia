import { useEffect, useState } from 'react';
import AddressCard from '../components/AddressCard';
import AddressFormModal from '../components/AddressFormModal';
import { PlusIcon } from '../components/Icons';
import { EmptyPinIllustration } from '../components/Illustrations';
import { createAddress, deleteAddress, listAddresses, updateAddress } from '../api/user';
import { useActionDialog } from '../hooks/useActionDialog';

export default function AddressesPage() {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { confirm, dialog } = useActionDialog();

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  function load() {
    setLoading(true);
    listAddresses()
      .then(setAddresses)
      .catch(() => setError('Could not load your addresses.'))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function openCreate() {
    setEditing(null);
    setFormError('');
    setModalOpen(true);
  }

  function openEdit(address) {
    setEditing(address);
    setFormError('');
    setModalOpen(true);
  }

  async function handleSubmit(form) {
    setSubmitting(true);
    setFormError('');
    try {
      if (editing) {
        await updateAddress(editing.id, form);
      } else {
        await createAddress(form);
      }
      setModalOpen(false);
      load();
    } catch {
      setFormError('Could not save this address — check the required fields.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(address) {
    if (!(await confirm(`Delete "${address.label}"?`, { title: 'Delete address', danger: true, confirmLabel: 'Delete' }))) return;
    await deleteAddress(address.id);
    load();
  }

  return (
    <div>
      {dialog}
      <div className="addresses-header">
        <div>
          <h1>Your addresses</h1>
          <p>Manage where merchants deliver your orders.</p>
        </div>
        {addresses.length > 0 && (
          <button className="btn btn-primary btn-sm" onClick={openCreate}>
            <PlusIcon style={{ width: 15, height: 15 }} /> Add new
          </button>
        )}
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : addresses.length === 0 ? (
        <div className="empty-state">
          <EmptyPinIllustration />
          <h3>No addresses yet</h3>
          <p>Add a delivery address so a merchant can find you once catalog is live.</p>
          <button className="btn btn-primary" style={{ marginTop: 8 }} onClick={openCreate}>
            <PlusIcon style={{ width: 16, height: 16 }} /> Add address
          </button>
        </div>
      ) : (
        <div className="address-grid">
          {addresses.map((a) => (
            <AddressCard key={a.id} address={a} onEdit={openEdit} onDelete={handleDelete} />
          ))}
        </div>
      )}

      {modalOpen && (
        <AddressFormModal
          initial={editing}
          onClose={() => setModalOpen(false)}
          onSubmit={handleSubmit}
          submitting={submitting}
          error={formError}
        />
      )}
    </div>
  );
}
