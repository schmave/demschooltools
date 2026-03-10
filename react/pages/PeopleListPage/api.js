import { getCsrfToken, jsonFetch } from '../../utils';

export const fetchPeople = () => jsonFetch('/api/people/');

export const fetchGridViews = (entityType = 'person') =>
  jsonFetch(`/api/grid-views/?entity_type=${encodeURIComponent(entityType)}`);

export const createGridView = (data) =>
  jsonFetch('/api/grid-views/', {
    method: 'POST',
    body: data,
  });

export const updateGridView = (id, data) =>
  jsonFetch(`/api/grid-views/${id}/`, {
    method: 'PATCH',
    body: data,
  });

export const deleteGridView = async (id) => {
  const response = await fetch(`/api/grid-views/${id}/`, {
    method: 'DELETE',
    credentials: 'same-origin',
    headers: {
      'X-CSRFToken': getCsrfToken(),
    },
  });

  if (response.status === 204) {
    return { deleted: true };
  }

  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const error = new Error('Failed to delete grid view');
    error.status = response.status;
    error.body = payload;
    throw error;
  }

  return { deleted: true };
};
