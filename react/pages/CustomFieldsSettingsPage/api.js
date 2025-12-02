import { getCsrfToken, jsonFetch } from '../../utils';

const ensureTrailingSlash = (url) => (url.endsWith('/') ? url : `${url}/`);

const buildDetailUrl = (baseUrl, id) => `${ensureTrailingSlash(baseUrl)}${id}/`;

export const fetchCustomFields = (baseUrl, entityType) => {
  const url = `${ensureTrailingSlash(baseUrl)}?entity_type=${encodeURIComponent(entityType)}`;
  return jsonFetch(url);
};

export const createCustomField = (baseUrl, payload) =>
  jsonFetch(ensureTrailingSlash(baseUrl), {
    method: 'POST',
    body: payload,
  });

export const updateCustomField = (baseUrl, id, payload) =>
  jsonFetch(buildDetailUrl(baseUrl, id), {
    method: 'PUT',
    body: payload,
  });

export const patchCustomField = (baseUrl, id, payload) =>
  jsonFetch(buildDetailUrl(baseUrl, id), {
    method: 'PATCH',
    body: payload,
  });

export const deleteCustomField = async (baseUrl, id) => {
  const response = await fetch(buildDetailUrl(baseUrl, id), {
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
  const payload = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    const error = new Error('Failed to delete custom field');
    error.status = response.status;
    error.body = payload;
    throw error;
  }

  return { deleted: false, field: payload };
};

export const fetchRoleKeys = (url) => jsonFetch(url);
