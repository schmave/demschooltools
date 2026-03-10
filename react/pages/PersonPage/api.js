import { getCsrfToken, jsonFetch } from '../../utils';

const ensureTrailingSlash = (url) => (url.endsWith('/') ? url : `${url}/`);

export const createPerson = (baseUrl, payload) =>
  jsonFetch(ensureTrailingSlash(baseUrl), {
    method: 'POST',
    body: payload,
  });

export const updatePerson = (baseUrl, id, payload) =>
  jsonFetch(`${ensureTrailingSlash(baseUrl)}${id}/`, {
    method: 'PUT',
    body: payload,
  });

export const getPerson = (baseUrl, id) =>
  jsonFetch(`${ensureTrailingSlash(baseUrl)}${id}/`);

export const deletePerson = async (baseUrl, id) => {
  const response = await fetch(`${ensureTrailingSlash(baseUrl)}${id}/`, {
    method: 'DELETE',
    credentials: 'same-origin',
    headers: {
      'X-CSRFToken': getCsrfToken(),
    },
  });

  if (response.status === 204) {
    return { deleted: true };
  }

  if (!response.ok) {
    const contentType = response.headers.get('content-type') || '';
    const body = contentType.includes('application/json')
      ? await response.json()
      : await response.text();
    const error = new Error('Failed to delete person');
    error.status = response.status;
    error.body = body;
    throw error;
  }

  return { deleted: true };
};
