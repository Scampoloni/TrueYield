import { writable } from 'svelte/store';

export type Toast = { id: string; message: string; type: 'success' | 'error'; };
export const toasts = writable<Toast[]>([]);

export function showToast(message: string, type: 'success' | 'error' = 'success') {
  const id = crypto.randomUUID();
  toasts.update(t => [...t, { id, message, type }]);
  setTimeout(() => toasts.update(t => t.filter(x => x.id !== id)), 3500);
}
