import { apiClient } from './client';

export function getUploadSignature() {
  return apiClient.get('/api/catalog/uploads/sign').then((r) => r.data);
}

/**
 * Uploads straight to Cloudinary from the browser — the image bytes never
 * pass through our own backend. The backend only ever hands out a short-lived
 * signature (see getUploadSignature above); Cloudinary itself verifies it.
 */
export async function uploadProductImage(file) {
  const sig = await getUploadSignature();

  const form = new FormData();
  form.append('file', file);
  form.append('api_key', sig.apiKey);
  form.append('timestamp', sig.timestamp);
  form.append('signature', sig.signature);
  form.append('folder', sig.folder);

  const res = await fetch(`https://api.cloudinary.com/v1_1/${sig.cloudName}/image/upload`, {
    method: 'POST',
    body: form,
  });
  if (!res.ok) {
    throw new Error('Image upload failed');
  }
  const data = await res.json();
  return data.secure_url;
}
