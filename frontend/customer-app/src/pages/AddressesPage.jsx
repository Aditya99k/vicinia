import { useEffect, useState } from 'react';
import AddressCard from '../components/AddressCard';
import AddressFormModal from '../components/AddressFormModal';
import { MapPinIcon, PlusIcon } from '../components/Icons';
import { createAddress, deleteAddress, listAddresses, updateAddress } from '../api/user';

export default function AddressesPage() {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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
    if (!window.confirm(`Delete "${address.label}"?`)) return;
    await deleteAddress(address.id);
    load();
  }

  return (
    <div>
      {error && <div className="banner banner-error">{error}</div>}

      {loading ? (
        <div className="page-loading"><span className="spinner" /> Loading…</div>
      ) : addresses.length === 0 ? (
        <div className="empty-state">
          <MapPinIcon />
          <h3>No addresses yet</h3>
          <p>Add a delivery address so a merchant can find you once catalog is live.</p>
          <button className="btn btn-primary" style={{ marginTop: 8 }} onClick={openCreate}>
            <PlusIcon style={{ width: 17, height: 17 }} /> Add address
          </button>
        </div>
      ) : (
        <>
          {addresses.map((a) => (
            <AddressCard key={a.id} address={a} onEdit={openEdit} onDelete={handleDelete} />
          ))}
          <div className="fab">
            <button className="btn btn-primary btn-sm" onClick={openCreate}>
              <PlusIcon style={{ width: 15, height: 15 }} /> Add new
            </button>
          </div>
        </>
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
