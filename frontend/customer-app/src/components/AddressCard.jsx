import { EditIcon, MapPinIcon, TrashIcon } from './Icons';

export default function AddressCard({ address, onEdit, onDelete }) {
  return (
    <div className="card address-card">
      <div className="icon">
        <MapPinIcon />
      </div>
      <div className="body">
        <div className="top-row">
          <span className="label-text">{address.label}</span>
          {address.isDefault && <span className="badge badge-success">Default</span>}
        </div>
        <div className="lines">
          {address.line1}
          {address.line2 ? `, ${address.line2}` : ''}
          <br />
          {address.city}, {address.state} — {address.pincode}
        </div>
        <div className="actions">
          <button className="edit" onClick={() => onEdit(address)}>
            <EditIcon style={{ width: 13, height: 13, verticalAlign: -2, marginRight: 4 }} />
            Edit
          </button>
          <button className="delete" onClick={() => onDelete(address)}>
            <TrashIcon style={{ width: 13, height: 13, verticalAlign: -2, marginRight: 4 }} />
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}
