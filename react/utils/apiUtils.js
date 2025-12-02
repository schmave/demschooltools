const CSRF_COOKIE_NAME = 'csrftoken';

export const getCsrfToken = () => {
  if (typeof document === 'undefined') {
    return '';
  }
  const cookies = document.cookie ? document.cookie.split(';') : [];
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=');
    if (name === CSRF_COOKIE_NAME) {
      return decodeURIComponent(value);
    }
  }
  return '';
};

export const jsonFetch = async (url, options = {}) => {
  const { body, headers, method = 'GET' } = options;
  const fetchOptions = {
    method,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRFToken': getCsrfToken(),
      ...headers,
    },
  };

  if (body !== undefined) {
    fetchOptions.body = typeof body === 'string' ? body : JSON.stringify(body);
  }

  const response = await fetch(url, fetchOptions);
  const contentType = response.headers.get('content-type') || '';
  let payload;
  if (contentType.includes('application/json')) {
    payload = await response.json();
  } else {
    payload = await response.text();
  }

  if (!response.ok) {
    const error = new Error('Request failed');
    error.status = response.status;
    error.body = payload;
    throw error;
  }

  return payload;
};
